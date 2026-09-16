package chat.server;

import chat.domain.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ConcurrentMap<String, ClientHandler> registeredUsers;
    private String username;
    private String room;
    private PrintWriter out;

    public ClientHandler(Socket socket, ConcurrentMap<String, ClientHandler> registeredUsers) {
        this.socket = socket;
        this.registeredUsers = registeredUsers;
        this.room = ChatServer.DEFAULT_ROOM;
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
                    sendServerMessage(Message.TYPE_ERROR, "server", null, "Du skal vælge et brugernavn først");
                    continue;
                }

                if (!Message.TYPE_TEXT.equals(message.getType())) {
                    sendServerMessage(Message.TYPE_ERROR, "server", null, "Ukendt meddelelsestype: " + message.getType());
                    continue;
                }

                String targetRoom = message.getRoom();
                if (targetRoom == null || targetRoom.isBlank() || room == null || !room.equals(targetRoom)) {
                    sendServerMessage(Message.TYPE_ERROR, "server", null, "Beskedens TARGET svarer ikke til dit registrerede rum");
                    continue;
                }

                System.out.println(clientAddr + " (" + username + ") -> " + message.getText() + " [" + room + "]");
                broadcastMessage(room, message.getText());
            }
        } catch (IOException e) {
            System.err.println("Connection error with " + clientAddr + ": " + e.getMessage());
        } finally {
            unregisterUsername();
            leaveRoom();
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            System.out.println("Connection closed: " + clientAddr);
        }
    }

    private void handleLogin(String requestedUsername) {
        if (requestedUsername == null) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, "Brugernavnet kan ikke være tomt");
            return;
        }

        String normalizedUsername = requestedUsername.trim();
        if (normalizedUsername.isEmpty()) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, "Brugernavnet kan ikke være tomt");
            return;
        }

        if (registeredUsers.putIfAbsent(normalizedUsername, this) != null) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, "Brugernavnet er optaget");
            return;
        }

        String previousUsername = this.username;
        if (previousUsername != null && !previousUsername.equals(normalizedUsername)) {
            registeredUsers.remove(previousUsername, this);
        }
        this.username = normalizedUsername;
        joinRoom(ChatServer.DEFAULT_ROOM);

        System.out.println(socket.getRemoteSocketAddress() + " registered username: " + username + " in room " + room);
        sendServerMessage(Message.TYPE_LOGIN, "server", null, "Brugernavnet er accepteret: " + username);
    }

    private void joinRoom(String targetRoom) {
        if (targetRoom == null || targetRoom.isBlank()) {
            targetRoom = ChatServer.DEFAULT_ROOM;
        }
        if (room != null && !room.equals(targetRoom)) {
            leaveRoom();
        }

        room = targetRoom;
        Set<ClientHandler> members = ChatServer.ROOMS.computeIfAbsent(room,
                key -> Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>()));
        members.add(this);
    }

    private void leaveRoom() {
        if (room == null) {
            return;
        }
        Set<ClientHandler> members = ChatServer.ROOMS.get(room);
        if (members != null) {
            members.remove(this);
            if (members.isEmpty()) {
                ChatServer.ROOMS.remove(room, members);
            }
        }
        room = null;
    }

    private void unregisterUsername() {
        if (username != null) {
            registeredUsers.remove(username, this);
            username = null;
        }
    }

    private void broadcastMessage(String roomToSend, String text) {
        if (username == null || text == null || roomToSend == null) {
            return;
        }

        Set<ClientHandler> roomMembers = ChatServer.ROOMS.get(roomToSend);
        if (roomMembers == null) {
            return;
        }

        for (ClientHandler client : roomMembers) {
            if (client != null) {
                client.sendServerMessage(Message.TYPE_TEXT, username, roomToSend, text);
            }
        }
    }

    private void sendServerMessage(String type, String sender, String room, String text) {
        if (out == null) {
            return;
        }
        synchronized (out) {
            out.println(Message.formatServerMessage(type, sender, room, text));
        }
    }
}
