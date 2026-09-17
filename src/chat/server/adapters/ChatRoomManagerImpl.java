package chat.server.adapters;

import chat.server.ChatServer;
import chat.server.ClientHandler;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomManagerImpl implements ChatRoomManager {
    @Override
    public void joinRoom(String room, ClientHandler member) {
        if (room == null || room.isBlank()) {
            room = ChatServer.DEFAULT_ROOM;
        }
        Set<ClientHandler> members = ChatServer.ROOMS.computeIfAbsent(room,
                key -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        members.add(member);
    }

    @Override
    public void leaveRoom(String room, ClientHandler member) {
        if (room == null) return;
        Set<ClientHandler> members = ChatServer.ROOMS.get(room);
        if (members != null) {
            members.remove(member);
            if (members.isEmpty()) {
                ChatServer.ROOMS.remove(room, members);
            }
        }
    }

    @Override
    public Set<ClientHandler> getMembers(String room) {
        Set<ClientHandler> members = ChatServer.ROOMS.get(room);
        return members == null ? Collections.emptySet() : members;
    }

    @Override
    public void broadcast(String room, String sender, String text) {
        Set<ClientHandler> members = getMembers(room);
        for (ClientHandler client : members) {
            if (client != null) {
                client.deliverServerMessage("TEXT", sender, room, text);
            }
        }
    }
}
