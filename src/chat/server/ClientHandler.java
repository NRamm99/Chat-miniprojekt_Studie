package chat.server;

import chat.domain.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message message = Message.fromProtocol(line);

                if (Message.TYPE_LOGIN.equals(message.getType())) {
                    username = message.getText();
                    System.out.println(clientAddr + " registered username: " + username);
                    continue;
                }

                String sender = username == null ? "unknown" : username;
                System.out.println(clientAddr + " (" + sender + ") -> " + message.getText());
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
