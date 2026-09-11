package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static final int DEFAULT_PORT = 5001;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        System.out.println("Starting ChatServer on port " + port);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ChatServer listening on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Accepted connection from " + client.getRemoteSocketAddress());
                pool.execute(new ClientHandler(client));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
            System.out.println("Server shutting down.");
        }
    }
}
