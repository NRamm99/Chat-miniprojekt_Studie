package chat.server.adapters;

import chat.application.ports.MessageGateway;
import chat.server.ClientHandler;

public class MessageDispatcherImpl implements MessageDispatcher, MessageGateway {
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
