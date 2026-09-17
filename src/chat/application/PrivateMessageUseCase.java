package chat.application;

import chat.server.adapters.ClientRegistry;
import chat.server.adapters.MessageDispatcher;

public class PrivateMessageUseCase {
    private final ClientRegistry registry;
    private final MessageDispatcher dispatcher;

    public PrivateMessageUseCase(ClientRegistry registry, MessageDispatcher dispatcher) {
        this.registry = registry;
        this.dispatcher = dispatcher;
    }

    public void send(String sender, String recipient, String text) {
        if (recipient == null || recipient.isBlank()) {
            dispatcher.sendErrorTo(sender, "Ugyldigt brugernavn for privat besked");
            return;
        }
        if (registry.get(recipient) == null) {
            dispatcher.sendErrorTo(sender, "Brugeren findes ikke: " + recipient);
            return;
        }
        dispatcher.sendPrivate(sender, recipient, text);
    }
}
