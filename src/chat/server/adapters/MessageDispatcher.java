package chat.server.adapters;

public interface MessageDispatcher {
    void sendPrivate(String sender, String recipient, String text);
    void sendErrorTo(String username, String text);
}
