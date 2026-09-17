package chat.server.adapters;

import chat.server.ClientHandler;

import java.util.concurrent.ConcurrentMap;

public class InMemoryClientRegistry implements ClientRegistry {
    private final ConcurrentMap<String, ClientHandler> backing;

    public InMemoryClientRegistry(ConcurrentMap<String, ClientHandler> backing) {
        this.backing = backing;
    }

    @Override
    public boolean register(String username, ClientHandler handler) {
        return backing.putIfAbsent(username, handler) == null;
    }

    @Override
    public void unregister(String username) {
        backing.remove(username);
    }

    @Override
    public ClientHandler get(String username) {
        return backing.get(username);
    }
}
