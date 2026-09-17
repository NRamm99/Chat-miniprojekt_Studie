package chat.application;

import chat.application.ports.MessageGateway;
import chat.application.ports.UserPresence;

public class PrivateMessageUseCase {
    private final UserPresence presence;
    private final MessageGateway messages;

    public PrivateMessageUseCase(UserPresence presence, MessageGateway messages) {
        this.presence = presence;
        this.messages = messages;
    }

    public void send(String sender, String recipient, String text) {
        if (recipient == null || recipient.isBlank()) {
            messages.sendErrorTo(sender, "Ugyldigt brugernavn for privat besked");
            return;
        }
        if (!presence.isOnline(recipient)) {
            messages.sendErrorTo(sender, "Brugeren findes ikke: " + recipient);
            return;
        }
        messages.sendPrivate(sender, recipient, text);
    }
}
