import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                // Log received message with client IP:port
                System.out.println(clientAddr + " -> " + line);
            }
        } catch (IOException e) {
            System.err.println("Connection error with " + clientAddr + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            System.out.println("Connection closed: " + clientAddr);
        }
    }
}
