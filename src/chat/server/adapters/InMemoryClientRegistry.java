package chat.server.adapters;

import chat.server.ClientHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryClientRegistry implements ClientRegistry {
    private final ConcurrentMap<String, ClientHandler> backing = new ConcurrentHashMap<>();

    public InMemoryClientRegistry() {
    }

    @Override
    public boolean register(String username, ClientHandler handler) {
        return backing.putIfAbsent(username, handler) == null;
    }

    @Override
    public void unregister(String username, ClientHandler handler) {
        if (username != null && handler != null) {
            backing.remove(username, handler);
        }
    }

    @Override
    public ClientHandler get(String username) {
        return backing.get(username);
    }
}
