package ClientServer;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private int port;
    private ConcurrentHashMap<String, ClientHandler> clientHandlers;
    private ExecutorService pool;
    private File logFile;

    public Server(int port) {
        this.port = port;
        clientHandlers = new ConcurrentHashMap<>();
        pool = Executors.newCachedThreadPool();
        logFile = new File("server_log.txt");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void logAction(String message) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = 12345; // Set your desired port here
        new Server(port).start();
    }

    // Inner class for handling client connections
    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Server server;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;
        private int counter;

        public ClientHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] tokens = inputLine.split(" ");
                    String command = tokens[0];

                    switch (command) {
                        case "REGISTER":
                            handleRegister(tokens);
                            break;
                        case "INCREASE":
                        case "DECREASE":
                            handleCounterUpdate(tokens);
                            break;
                        case "LOGOUT":
                            handleLogout();
                            break;
                        default:
                            out.println("ERROR Unknown command");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRegister(String[] tokens) {
            if (tokens.length != 3) {
                out.println("ERROR Invalid registration format");
                return;
            }

            String id = tokens[1];
            if (clientHandlers.containsKey(id)) {
                out.println("ERROR ID already registered");
                return;
            }

            clientId = id;
            clientHandlers.put(id, this);
            out.println("ACK");
        }

        private void handleCounterUpdate(String[] tokens) {
            if (tokens.length != 3) {
                out.println("ERROR Invalid command format");
                return;
            }

            int amount = Integer.parseInt(tokens[1]);
            if ("DECREASE".equals(tokens[0])) {
                amount = -amount;
            }

            counter += amount;
            server.logAction("Client " + clientId + " updated counter to " + counter);
            out.println("Counter updated to " + counter);
        }

        private void handleLogout() {
            clientHandlers.remove(clientId);
            out.println("Logged out");
        }
    }
}
