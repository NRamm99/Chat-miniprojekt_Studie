package chat.server;

import chat.server.adapters.ChatRoomManagerImpl;
import chat.server.adapters.DefaultMessageParser;
import chat.server.adapters.InMemoryClientRegistry;
import chat.server.adapters.MessageDispatcherImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger LOG = Logger.getLogger(ChatServer.class.getName());
    public static final int DEFAULT_PORT = 5001;
    public static final String DEFAULT_ROOM = "lobby";
    public static final String SECOND_ROOM = "room67";
    public static final ConcurrentMap<String, ClientHandler> REGISTERED_USERS = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, Set<ClientHandler>> ROOMS = new ConcurrentHashMap<>();

    static {
        ROOMS.put(DEFAULT_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        ROOMS.put(SECOND_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<>()));
    }

    public static void main(@SuppressWarnings("unused") String[] args) {
        int port = DEFAULT_PORT;
        LOG.info("Starting ChatServer on port " + port);
        LOG.fine("Available rooms: " + DEFAULT_ROOM + ", " + SECOND_ROOM);

        // Create shared adapters
        InMemoryClientRegistry registry = new InMemoryClientRegistry(REGISTERED_USERS);
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(registry);
        ChatRoomManagerImpl roomManager = new ChatRoomManagerImpl();
        DefaultMessageParser parser = new DefaultMessageParser();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ExecutorService pool = Executors.newFixedThreadPool(3);
            try {
                LOG.info("ChatServer listening on port " + port);
                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    LOG.fine("Accepted connection from " + client.getRemoteSocketAddress());
                    pool.execute(new ClientHandler(client, registry, dispatcher, roomManager, parser));
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