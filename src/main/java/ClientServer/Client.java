package ClientServer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

public class Client implements Runnable {

    private String id;
    private String password;
    private String serverIp;
    private int serverPort;
    private int delay;
    private String[] actions;

    public static void main(String[] args) throws IOException, InterruptedException {
        String[] users = {"clients/client1.json", "clients/client2.json"};

        int i = 0;
        for (String user : users) {
            if(i++ > 1000) System.exit(0);
            Client c = new Client(user);
            {
                new Thread(c).start();
                Thread.sleep(1000L);
            }

        }
    }

    public Client(String path) throws IOException {
        // Parse the JSON configuration file
        String content = new String(Files.readAllBytes(Paths.get(path)));
        JSONObject json = new JSONObject(content);
        Security.addProvider(new BouncyCastleProvider());

        this.id = json.getString("id");
        this.password = json.getString("password");
        System.out.println("Client " + id + "passwd " + password);
        JSONObject server = json.getJSONObject("server");
        this.serverIp = server.getString("ip");
        this.serverPort = server.getInt("port");
        JSONObject actions = json.getJSONObject("actions");
        this.delay = actions.getInt("delay");
        // assert delay at least 2 seconds
        if (this.delay < 2) {
            this.delay = 2;
        }

        this.actions = actions.getJSONArray("steps").toList().toArray(new String[0]);
    }



    private String hashPassword(String Password) throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
        digest.update(password.getBytes());
        byte[] hashedPassword = digest.digest();
        return bytesToHex(hashedPassword);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    @Override
    public void run() {
        try (Socket socket = new Socket(serverIp, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                Thread.sleep(delay * 1000L);
                out.println("REGISTER " + id + " " + hashPassword(password));

                String response = in.readLine();
                if (!response.equals("ACK")) {
                    throw new IOException("Registration failed: " + response);
                }

                // Perform actions
                for (String action : actions) {
                    Thread.sleep(delay * 1000L);
                    out.println(action + " " + id);
                    System.out.println("Server response: " + in.readLine());
                }

            // Logout
            out.println("LOGOUT " + id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
