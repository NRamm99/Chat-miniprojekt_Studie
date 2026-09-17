package chat.server;

import chat.adapters.DefaultMessageParser;
import chat.application.GetRoomHistoryUseCase;
import chat.application.JoinRoomUseCase;
import chat.application.LoginUseCase;
import chat.application.PrivateMessageUseCase;
import chat.application.RecordRoomMessageUseCase;
import chat.server.adapters.ChatRoomManagerImpl;
import chat.server.adapters.InMemoryClientRegistry;
import chat.server.adapters.InMemoryMessageHistory;
import chat.server.adapters.MessageDispatcherImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger LOG = Logger.getLogger(ChatServer.class.getName());
    public static final int DEFAULT_PORT = 5001;
    public static final String DEFAULT_ROOM = "lobby";
    public static final String SECOND_ROOM = "room67";

    public static void main(@SuppressWarnings("unused") String[] args) {
        int port = DEFAULT_PORT;
        LOG.info("Starting ChatServer on port " + port);
        LOG.fine("Available rooms: " + DEFAULT_ROOM + ", " + SECOND_ROOM);

        InMemoryClientRegistry registry = new InMemoryClientRegistry();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(registry);
        ChatRoomManagerImpl roomManager = new ChatRoomManagerImpl();
        InMemoryMessageHistory messageHistory = new InMemoryMessageHistory();
        DefaultMessageParser parser = new DefaultMessageParser();
        LoginUseCase loginUseCase = new LoginUseCase();
        JoinRoomUseCase joinRoomUseCase = new JoinRoomUseCase();
        PrivateMessageUseCase privateMessageUseCase = new PrivateMessageUseCase(registry, dispatcher);
        RecordRoomMessageUseCase recordRoomMessageUseCase = new RecordRoomMessageUseCase(messageHistory);
        GetRoomHistoryUseCase getRoomHistoryUseCase = new GetRoomHistoryUseCase(messageHistory);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ExecutorService pool = Executors.newFixedThreadPool(3);
            try {
                LOG.info("ChatServer listening on port " + port);
                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    LOG.fine("Accepted connection from " + client.getRemoteSocketAddress());
                    pool.execute(new ClientHandler(
                            client,
                            registry,
                            roomManager,
                            parser,
                            loginUseCase,
                            joinRoomUseCase,
                            privateMessageUseCase,
                            recordRoomMessageUseCase,
                            getRoomHistoryUseCase));
                }
            } finally {
                pool.shutdown();
            }
        } catch (IOException e) {
            LOG.severe("Server error: " + e.getMessage());
        } finally {
            LOG.info("Server shutting down.");
        }
    }
}
