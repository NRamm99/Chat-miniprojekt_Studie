package chat.server.adapters;

import chat.server.ClientHandler;

import java.util.Set;

public interface ChatRoomManager {
    void joinRoom(String room, ClientHandler member);
    void leaveRoom(String room, ClientHandler member);
    Set<ClientHandler> getMembers(String room);
    void broadcast(String room, String sender, String text);
}
