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

public class ChatServer {
    public static final int DEFAULT_PORT = 5001;
    public static final String DEFAULT_ROOM = "lobby";
    public static final String SECOND_ROOM = "room67";
    public static final ConcurrentMap<String, ClientHandler> REGISTERED_USERS = new ConcurrentHashMap<String, ClientHandler>();
    public static final ConcurrentMap<String, Set<ClientHandler>> ROOMS = new ConcurrentHashMap<String, Set<ClientHandler>>();

    static {
        ROOMS.put(DEFAULT_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<ClientHandler, Boolean>()));
        ROOMS.put(SECOND_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<ClientHandler, Boolean>()));
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        System.out.println("Starting ChatServer on port " + port);
        System.out.println("Available rooms: " + DEFAULT_ROOM + ", " + SECOND_ROOM);

        // Create shared adapters
        InMemoryClientRegistry registry = new InMemoryClientRegistry(REGISTERED_USERS);
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(registry);
        ChatRoomManagerImpl roomManager = new ChatRoomManagerImpl();
        DefaultMessageParser parser = new DefaultMessageParser();

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ChatServer listening on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Accepted connection from " + client.getRemoteSocketAddress());
                pool.execute(new ClientHandler(client, registry, dispatcher, roomManager, parser));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
            System.out.println("Server shutting down.");
        }
    }
}
