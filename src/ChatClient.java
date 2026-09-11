import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5001;

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to ChatServer at " + host + ":" + port + ". Type messages and press Enter to send. Ctrl+D (or Ctrl+Z then Enter on Windows) to exit.");
            String line;
            while ((line = console.readLine()) != null) {
                // Send in required format
                out.println("TEXT||" + line);
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
