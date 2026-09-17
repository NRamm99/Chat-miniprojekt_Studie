package chat.application.ports;

public interface MessageGateway {
    void sendPrivate(String sender, String recipient, String text);
    void sendErrorTo(String username, String text);
}
