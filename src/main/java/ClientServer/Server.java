package ClientServer;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    /**
     * SET DESIRED PORT HERE
     */
    private static final int PORT = 12345;
    private ConcurrentHashMap<String, ClientHandler> clientHandlers;
    private ExecutorService pool;
    private File logFile;

    public static void main(String[] args) {
        new Server().start();
    }

    public Server() {
        clientHandlers = new ConcurrentHashMap<>();
        pool = Executors.newCachedThreadPool();
        logFile = new File("server_log.txt");
    }

    /**
     * Starts the server and listens for client connections.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs an action to the server log file.
     * @param message The message to log.
     */
    public synchronized void logAction(String message) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Inner class that handles a client connection.
     */
    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Server server;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;
        private String password;

        private int counter;

        /**
         * Creates a new ClientHandler.
         * @param socket The client socket.
         * @param server The server.
         */
        public ClientHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;
        }

        /**
         * Runs the client handler.
         */
        @Override
        public void run() {
            // Open input and output streams
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                // Read commands from the client
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Inputline: " + inputLine);
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

        /**
         * Handles a REGISTER command.
         * @param tokens The tokens of the command.
         */
        private void handleRegister(String[] tokens) {
            if (tokens.length != 3) {
                out.println("ERROR Invalid registration format");
                return;
            }

            String id = tokens[1];
            String providedPassword = tokens[2];


            if (clientHandlers.containsKey(id) && !clientHandlers.get(id).password.equals(providedPassword)) {
                ClientHandler existingClient = clientHandlers.get(id);
                System.out.println(existingClient.password);
                System.out.println(providedPassword);
                if (!existingClient.password.equals(providedPassword)) {
                    out.println("ERROR Cannot log in Client: " + id + " already exists");
                }
            } else {
                // New registration
                this.password = providedPassword; // Store the hashed password
                clientId = id;
                System.out.println(id);
                System.out.println(clientHandlers);
                clientHandlers.put(id, this);
                out.println("ACK");
            }

        }

        /**
         * Handles an INCREASE or DECREASE command.
         * @param tokens The tokens of the command.
         */
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

        /**
         * Handles a LOGOUT command.
         */
        private void handleLogout() {
            clientHandlers.remove(clientId);
            out.println("Logged out");
        }
    }
}
