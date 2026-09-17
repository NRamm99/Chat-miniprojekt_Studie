package chat.application.ports;

public interface RoomMembership {
    void join(String room);
    void leave(String room);
}
