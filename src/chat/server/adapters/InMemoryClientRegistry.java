package chat.server.adapters;

import chat.application.ports.UserPresence;
import chat.server.ClientHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryClientRegistry implements ClientRegistry, UserPresence {
    private final ConcurrentMap<String, ClientHandler> users = new ConcurrentHashMap<>();

    @Override
    public boolean register(String username, ClientHandler handler) {
        return users.putIfAbsent(username, handler) == null;
    }

    @Override
    public void unregister(String username, ClientHandler handler) {
        if (username != null && handler != null) {
            users.remove(username, handler);
        }
    }

    @Override
    public ClientHandler get(String username) {
        return users.get(username);
    }

    @Override
    public boolean isOnline(String username) {
        return username != null && users.containsKey(username);
    }
}
