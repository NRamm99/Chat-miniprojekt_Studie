package chat.server;

import chat.adapters.MessageParser;
import chat.application.ActionResult;
import chat.application.GetRoomHistoryUseCase;
import chat.application.JoinRoomUseCase;
import chat.application.LoginUseCase;
import chat.application.PrivateMessageUseCase;
import chat.application.RecordRoomMessageUseCase;
import chat.domain.Message;
import chat.domain.StoredMessage;
import chat.server.adapters.ChatRoomManager;
import chat.server.adapters.ClientRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private final ClientRegistry registry;
    private final ChatRoomManager roomManager;
    private final MessageParser parser;
    private final LoginUseCase loginUseCase;
    private final JoinRoomUseCase joinRoomUseCase;
    private final PrivateMessageUseCase privateMessageUseCase;
    private final RecordRoomMessageUseCase recordRoomMessageUseCase;
    private final GetRoomHistoryUseCase getRoomHistoryUseCase;
    private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

    private volatile String username;
    private volatile String room;
    private volatile BufferedReader in;
    private volatile PrintWriter out;

    public ClientHandler(Socket socket,
                         ClientRegistry registry,
                         ChatRoomManager roomManager,
                         MessageParser parser,
                         LoginUseCase loginUseCase,
                         JoinRoomUseCase joinRoomUseCase,
                         PrivateMessageUseCase privateMessageUseCase,
                         RecordRoomMessageUseCase recordRoomMessageUseCase,
                         GetRoomHistoryUseCase getRoomHistoryUseCase) {
        this.socket = socket;
        this.registry = registry;
        this.roomManager = roomManager;
        this.parser = parser;
        this.loginUseCase = loginUseCase;
        this.joinRoomUseCase = joinRoomUseCase;
        this.privateMessageUseCase = privateMessageUseCase;
        this.recordRoomMessageUseCase = recordRoomMessageUseCase;
        this.getRoomHistoryUseCase = getRoomHistoryUseCase;
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
                recordRoomMessageUseCase.record(username, room, message.getText());
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
        ActionResult result = loginUseCase.execute(
                username,
                requestedUsername,
                ChatServer.DEFAULT_ROOM,
                name -> registry.register(name, this),
                roomName -> roomManager.joinRoom(roomName, this));

        if (!result.isSuccess()) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, result.getErrorMessage());
            return;
        }

        this.username = result.getValue();
        LOG.info(socket.getRemoteSocketAddress() + " registered username: " + username + " in room " + room);
        sendServerMessage(Message.TYPE_LOGIN, "server", null, "Brugernavnet er accepteret: " + username);
        sendRoomHistory(this.room);
    }

    private void handleJoinRoom(String targetRoom) {
        ActionResult result = joinRoomUseCase.execute(
                room,
                targetRoom,
                roomManager::exists,
                new chat.application.ports.RoomMembership() {
                    @Override
                    public void join(String roomName) {
                        roomManager.joinRoom(roomName, ClientHandler.this);
                    }

                    @Override
                    public void leave(String roomName) {
                        roomManager.leaveRoom(roomName, ClientHandler.this);
                    }
                });

        if (!result.isSuccess()) {
            sendServerMessage(Message.TYPE_ERROR, "server", null, result.getErrorMessage());
            return;
        }

        this.room = result.getValue();
        LOG.info(socket.getRemoteSocketAddress() + " (" + username + ") switched to room: " + room);
        sendServerMessage(Message.TYPE_JOIN_ROOM, "server", room, "Du er nu i rum " + room);
        sendRoomHistory(this.room);
    }

    public void deliverServerMessage(String type, String sender, String room, String text) {
        deliverServerMessage(type, sender, room, text, null);
    }

    public void deliverServerMessage(String type, String sender, String room, String text, LocalDateTime timestamp) {
       PrintWriter writer = out;
       if (writer == null || cleanedUp.get()) {
           return;
       }
       synchronized (writer) {
           if (cleanedUp.get()) {
               return;
           }
           writer.println(parser.format(type, sender, room, text, timestamp));
           writer.flush();
           if (writer.checkError()) {
               closeConnection();
           }
       }
    }

    private void sendRoomHistory(String roomName) {
        for (StoredMessage stored : getRoomHistoryUseCase.recent(roomName)) {
            deliverServerMessage(Message.TYPE_TEXT, stored.getSender(), stored.getRoom(), stored.getText(), stored.getTimestamp());
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

        Socket localSocket = socket;
        BufferedReader localReader = in;
        PrintWriter localWriter = out;

        try {
            if (localSocket != null && !localSocket.isClosed()) {
                localSocket.close();
            }
        } catch (IOException e) {
            LOG.fine("Could not close client socket: " + e.getMessage());
        }

        if (localReader != null) {
            try {
                localReader.close();
            } catch (IOException e) {
                LOG.fine("Could not close client input: " + e.getMessage());
            }
        }
        if (localWriter != null) {
            localWriter.close();
        }

        if (registeredUsername != null) {
            registry.unregister(registeredUsername, this);
        }
        if (registeredRoom != null) {
            roomManager.leaveRoom(registeredRoom, this);
        }

        in = null;
        out = null;
    }

    private void handlePrivateMessage(Message message) {
        if (username == null) {
            deliverServerMessage(Message.TYPE_ERROR, "server", "", "Du skal være logget ind for at sende private beskeder");
            return;
        }
        String recipient = message.getRoom();
        privateMessageUseCase.send(this.username, recipient, message.getText());
    }
}
