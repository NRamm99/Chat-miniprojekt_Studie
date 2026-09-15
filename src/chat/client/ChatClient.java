package chat.client;

import chat.domain.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    public static final String HOST = "localhost";
    public static final int PORT = 5001;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.print("Choose a username: ");
            String username = console.readLine();
            while (username == null || username.trim().isEmpty()) {
                System.out.print("Choose a username: ");
                username = console.readLine();
            }

            out.println(Message.fromLogin(username.trim()).toProtocolString());
            System.out.println("Connected to ChatServer at " + HOST + ":" + PORT + ". Type messages and press Enter to send. Ctrl+D (or Ctrl+Z then Enter on Windows) to exit.");

            String line;
            while ((line = console.readLine()) != null) {
                out.println(Message.fromText(line).toProtocolString());
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
