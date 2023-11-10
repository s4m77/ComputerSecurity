package ClientServer;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

public class Client {

    private String id;
    private String password;
    private String serverIp;
    private int serverPort;
    private int delay;
    private String[] actions;

    public Client(String path) throws IOException {
        // Parse the JSON configuration file
        String content = new String(Files.readAllBytes(Paths.get(path)));
        JSONObject json = new JSONObject(content);

        this.id = json.getString("id");
        this.password = json.getString("password");
        JSONObject server = json.getJSONObject("server");
        this.serverIp = server.getString("ip");
        this.serverPort = server.getInt("port");
        JSONObject actions = json.getJSONObject("actions");
        this.delay = actions.getInt("delay");
        this.actions = actions.getJSONArray("steps").toList().toArray(new String[0]);
    }

    public void start() {
        try (Socket socket = new Socket(serverIp, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("REGISTER " + id + " " + password);
            String response = in.readLine();
            if (!response.equals("ACK")) {
                throw new IOException("Registration failed: " + response);
            }

            // Perform actions
            for (String action : actions) {
                Thread.sleep(delay * 1000);
                out.println(action + " " + id);
                System.out.println("Server response: " + in.readLine());
            }

            // Logout
            out.println("LOGOUT " + id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String examplePath = "clients/example_config.json";
        Client client = new Client(examplePath);
        client.start();
    }
}
