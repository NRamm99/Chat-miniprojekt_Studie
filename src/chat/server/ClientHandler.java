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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private final ClientRegistry registry;
    private final MessageDispatcher dispatcher;
    private final ChatRoomManager roomManager;
    private final MessageParser parser;
    private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

    private volatile String username;
    private volatile String room;
    private volatile BufferedReader in;
    private volatile PrintWriter out;

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
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                Message message;
                try {
                    message = parser.parse(line);
                } catch (IllegalArgumentException e) {
                    sendError(e.getMessage());
                    continue;
                }

                if (Message.TYPE_QUIT.equals(message.getType())) {
                    break;
                }

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
                   sendError("Ukendt meddelelsestype: " + message.getType());
                   continue;
                }

                String targetRoom = message.getRoom();
                if (targetRoom == null || targetRoom.isBlank() || room == null || !room.equals(targetRoom)) {
                   sendError("Beskedens TARGET svarer ikke til dit registrerede rum");
                   continue;
                }

                LOG.fine(clientAddr + " (" + username + ") -> " + message.getText() + " [" + room + "]");
                roomManager.broadcast(room, username, message.getText());
            }
        } catch (IOException e) {
            if (!cleanedUp.get()) {
                LOG.warning("Connection error with " + clientAddr + ": " + e.getMessage());
            }
        } finally {
            closeConnection();
            LOG.info("Connection closed: " + clientAddr);
        }
    }

    private void handleLogin(String requestedUsername) {
        if (username != null) {
           sendServerMessage(Message.TYPE_ERROR, "server", null, "Denne forbindelse har allerede et accepteret brugernavn");
           return;
        }

        if (requestedUsername == null) {
           sendServerMessage(Message.TYPE_ERROR, "server", null, "Brugernavnet kan ikke være tomt");
            return;
        }

        String normalizedUsername = requestedUsername.trim();
        if (normalizedUsername.isEmpty()) {
           sendServerMessage(Message.TYPE_ERROR, "server", null, "Brugernavnet kan ikke være tomt");
            return;
        }

        // Reject usernames containing the protocol separator '|' to avoid breaking message field parsing later.
        if (normalizedUsername.indexOf('|') >= 0) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, "Ugyldigt brugernavn: indeholder forbudt tegn '|' ");
            return;
        }

        if (!registry.register(normalizedUsername, this)) {
           sendServerMessage(Message.TYPE_ERROR, "server", null, "Brugernavnet er optaget");
            return;
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
       PrintWriter writer = out;
       if (writer == null || cleanedUp.get()) {
           return;
       }
       synchronized (writer) {
           if (cleanedUp.get()) {
               return;
           }
           writer.println(parser.format(type, sender, room, text));
           writer.flush();
           if (writer.checkError()) {
               closeConnection();
           }
       }
    }

    private void sendServerMessage(String type, String sender, String room, String text) {
        if (Message.TYPE_ERROR.equals(type) && room == null) {
            room = username;
        }
        deliverServerMessage(type, sender, room, text);
    }

    private void sendError(String description) {
        sendServerMessage(Message.TYPE_ERROR, "server", null, description);
    }

    public void closeConnection() {
        if (!cleanedUp.compareAndSet(false, true)) {
            return;
        }

        String registeredUsername = username;
        String registeredRoom = room;
        username = null;
        room = null;

        registry.unregister(registeredUsername, this);
        roomManager.leaveRoom(registeredRoom, this);

        BufferedReader reader = in;
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                LOG.fine("Could not close client input: " + e.getMessage());
            }
        }
        PrintWriter writer = out;
        if (writer != null) {
            writer.close();
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOG.fine("Could not close client socket: " + e.getMessage());
        }
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
