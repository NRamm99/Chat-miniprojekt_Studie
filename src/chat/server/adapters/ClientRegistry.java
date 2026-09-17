package chat.server.adapters;

import chat.server.ClientHandler;

public interface ClientRegistry {
    boolean register(String username, ClientHandler handler);
    void unregister(String username, ClientHandler handler);
    ClientHandler get(String username);
}
