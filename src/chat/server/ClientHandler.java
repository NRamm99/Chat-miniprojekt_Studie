package chat.server;

import chat.domain.Message;
import chat.server.adapters.ChatRoomManager;
import chat.server.adapters.ClientRegistry;
import chat.server.adapters.MessageDispatcher;
import chat.server.adapters.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private final ClientRegistry registry;
    private final MessageDispatcher dispatcher;
    private final ChatRoomManager roomManager;
    private final MessageParser parser;

    private String username;
    private String room;
    private PrintWriter out;

    public ClientHandler(Socket socket,
                         ClientRegistry registry,
                         MessageDispatcher dispatcher,
                         ChatRoomManager roomManager,
                         MessageParser parser) {
        this.socket = socket;
        this.registry = registry;
        this.dispatcher = dispatcher;
        this.roomManager = roomManager;
        this.parser = parser;
        this.room = ChatServer.DEFAULT_ROOM;
    }

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                Message message = parser.parse(line);

                if (Message.TYPE_LOGIN.equals(message.getType())) {
                   handleLogin(message.getText());
                   continue;
                }

                if (username == null) {
                   sendServerMessage(Message.TYPE_ERROR, "server", null, "Du skal vælge et brugernavn først");
                   continue;
                }

                if (Message.TYPE_JOIN_ROOM.equals(message.getType())) {
                   handleJoinRoom(message.getText());
                   continue;
                }

                if (Message.TYPE_PRIVATE.equals(message.getType())) {
                   handlePrivateMessage(message);
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

                LOG.fine(clientAddr + " (" + username + ") -> " + message.getText() + " [" + room + "]");
                roomManager.broadcast(room, username, message.getText());
            }
        } catch (IOException e) {
            LOG.warning("Connection error with " + clientAddr + ": " + e.getMessage());
        } finally {
            unregisterUsername();
            if (room != null) {
                roomManager.leaveRoom(room, this);
            }

            try {
                socket.close();
            } catch (IOException ignore) {
            }
            LOG.info("Connection closed: " + clientAddr);
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

        if (!registry.register(normalizedUsername, this)) {
           sendServerMessage(Message.TYPE_ERROR, "server", null, "Brugernavnet er optaget");
            return;
        }

        String previousUsername = this.username;
        if (previousUsername != null && !previousUsername.equals(normalizedUsername)) {
            registry.unregister(previousUsername);
        }
        this.username = normalizedUsername;
        roomManager.joinRoom(ChatServer.DEFAULT_ROOM, this);

        LOG.info(socket.getRemoteSocketAddress() + " registered username: " + username + " in room " + room);
        sendServerMessage(Message.TYPE_LOGIN, "server", null, "Brugernavnet er accepteret: " + username);
    }

    // removed unused helper joinRoom — room management is handled via roomManager directly

    private void leaveRoom() {
       if (room == null) {
           return;
       }
       roomManager.leaveRoom(room, this);
       room = null;
    }

    private void unregisterUsername() {
       if (username != null) {
           registry.unregister(username);
           username = null;
       }
    }

    private void handleJoinRoom(String targetRoom) {
        if (targetRoom == null || targetRoom.isBlank()) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, "Rumnavnet kan ikke være tomt");
            return;
        }

        if (room != null && room.equals(targetRoom)) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, "Du er allerede i det rum");
            return;
        }

        // validate room existence via ChatServer.ROOMS map still used by ChatRoomManagerImpl
        java.util.Set<ClientHandler> targetRoomMembers = ChatServer.ROOMS.get(targetRoom);
        if (targetRoomMembers == null) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, "Rummet findes ikke");
            return;
        }

        leaveRoom();
        room = targetRoom;
        roomManager.joinRoom(room, this);

        LOG.info(socket.getRemoteSocketAddress() + " (" + username + ") switched to room: " + room);
        sendServerMessage(Message.TYPE_JOIN_ROOM, "server", room, "Du er nu i rum " + room);
    }

    public void deliverServerMessage(String type, String sender, String room, String text) {
       if (out == null) {
           return;
       }
       synchronized (out) {
           out.println(parser.format(type, sender, room, text));
       }
    }

    private void sendServerMessage(String type, String sender, String room, String text) {
        deliverServerMessage(type, sender, room, text);
    }

    private void handlePrivateMessage(Message message) {
        if (username == null) {
            deliverServerMessage(Message.TYPE_ERROR, "server", "", "Du skal være logget ind for at sende private beskeder");
            return;
        }
        String recipient = message.getRoom();
        chat.application.PrivateMessageUseCase useCase = new chat.application.PrivateMessageUseCase(registry, dispatcher);
        useCase.send(this.username, recipient, message.getText());
    }

}
