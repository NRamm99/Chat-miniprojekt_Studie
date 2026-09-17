package chat.server.adapters;

import chat.server.ChatServer;
import chat.server.ClientHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class ChatRoomManagerImpl implements ChatRoomManager {
    private static final Logger LOG = Logger.getLogger(ChatRoomManagerImpl.class.getName());
    private final ConcurrentMap<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();

    public ChatRoomManagerImpl() {
        rooms.put(ChatServer.DEFAULT_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        rooms.put(ChatServer.SECOND_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<>()));
    }

    @Override
    public void joinRoom(String room, ClientHandler member) {
        if (room == null || room.isBlank()) {
            room = ChatServer.DEFAULT_ROOM;
        }
        Set<ClientHandler> members = rooms.computeIfAbsent(room,
                key -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        members.add(member);
    }

    @Override
    public void leaveRoom(String room, ClientHandler member) {
        if (room == null) return;
        Set<ClientHandler> members = rooms.get(room);
        if (members != null) {
            members.remove(member);
            if (members.isEmpty()
                    && !ChatServer.DEFAULT_ROOM.equals(room)
                    && !ChatServer.SECOND_ROOM.equals(room)) {
                rooms.remove(room, members);
            }
        }
    }

    @Override
    public boolean roomExists(String room) {
        return room != null && rooms.containsKey(room);
    }

    @Override
    public Set<ClientHandler> getMembers(String room) {
        if (room == null) {
            return Collections.emptySet();
        }
        Set<ClientHandler> members = rooms.get(room);
        if (members == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(members));
    }

    @Override
    public void broadcast(String room, String sender, String text) {
        Set<ClientHandler> members = getMembers(room);
        for (ClientHandler client : members) {
            if (client != null) {
                try {
                    client.deliverServerMessage("TEXT", sender, room, text);
                } catch (RuntimeException e) {
                    LOG.warning("Could not deliver broadcast: " + e.getMessage());
                    client.closeConnection();
                }
            }
        }
    }
}
