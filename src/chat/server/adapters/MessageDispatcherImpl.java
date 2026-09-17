package chat.server.adapters;

import chat.server.ClientHandler;

public class MessageDispatcherImpl implements MessageDispatcher {
    private final ClientRegistry registry;

    public MessageDispatcherImpl(ClientRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void sendPrivate(String sender, String recipient, String text) {
        ClientHandler target = registry.get(recipient);
        if (target != null) {
            target.deliverServerMessage("PRIVATE", sender, recipient, text);
        }
    }

    @Override
    public void sendErrorTo(String username, String text) {
        ClientHandler target = registry.get(username);
        if (target != null) {
            target.deliverServerMessage("ERROR", "server", username, text);
        }
    }
}
