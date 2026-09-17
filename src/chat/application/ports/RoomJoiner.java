package chat.application.ports;

@FunctionalInterface
public interface RoomJoiner {
    void join(String room);
}
