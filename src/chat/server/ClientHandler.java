package chat.server;

import chat.domain.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ConcurrentMap<String, ClientHandler> registeredUsers;
    private String username;
    private PrintWriter out;

    public ClientHandler(Socket socket, ConcurrentMap<String, ClientHandler> registeredUsers) {
        this.socket = socket;
        this.registeredUsers = registeredUsers;
    }

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                Message message = Message.fromProtocol(line);

                if (Message.TYPE_LOGIN.equals(message.getType())) {
                    handleLogin(message.getText());
                    continue;
                }

                if (username == null) {
                    sendServerMessage(Message.TYPE_ERROR, "server", "Du skal vælge et brugernavn først");
                    continue;
                }

                if (!Message.TYPE_TEXT.equals(message.getType())) {
                    sendServerMessage(Message.TYPE_ERROR, "server", "Ukendt meddelelsestype: " + message.getType());
                    continue;
                }

                System.out.println(clientAddr + " (" + username + ") -> " + message.getText());
                broadcastMessage(message.getText());
            }
        } catch (IOException e) {
            System.err.println("Connection error with " + clientAddr + ": " + e.getMessage());
        } finally {
            unregisterUsername();
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            System.out.println("Connection closed: " + clientAddr);
        }
    }

    private void handleLogin(String requestedUsername) {
        if (requestedUsername == null) {
            sendServerMessage(Message.TYPE_ERROR, "server", "Brugernavnet kan ikke være tomt");
            return;
        }

        String normalizedUsername = requestedUsername.trim();
        if (normalizedUsername.isEmpty()) {
            sendServerMessage(Message.TYPE_ERROR, "server", "Brugernavnet kan ikke være tomt");
            return;
        }

        if (registeredUsers.putIfAbsent(normalizedUsername, this) != null) {
            sendServerMessage(Message.TYPE_ERROR, "server", "Brugernavnet er optaget");
            return;
        }

        String previousUsername = this.username;
        this.username = normalizedUsername;
        if (previousUsername != null && !previousUsername.equals(normalizedUsername)) {
            registeredUsers.remove(previousUsername);
        }

        System.out.println(socket.getRemoteSocketAddress() + " registered username: " + username);
        sendServerMessage(Message.TYPE_LOGIN, "server", "Brugernavnet er accepteret: " + username);
    }

    private void unregisterUsername() {
        if (username != null) {
            registeredUsers.remove(username, this);
            username = null;
        }
    }

    private void broadcastMessage(String text) {
        if (username == null || text == null) {
            return;
        }

        for (ClientHandler client : registeredUsers.values()) {
            if (client != null) {
                client.sendServerMessage(Message.TYPE_TEXT, username, text);
            }
        }
    }

    private void sendServerMessage(String type, String sender, String text) {
        if (out == null) {
            return;
        }
        synchronized (out) {
            out.println(Message.formatServerMessage(type, sender, text));
        }
    }
}
