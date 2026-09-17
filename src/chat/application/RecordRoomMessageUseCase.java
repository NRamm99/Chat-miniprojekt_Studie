package chat.application;

import chat.application.ports.MessageHistory;
import chat.domain.StoredMessage;

import java.time.LocalDateTime;

public class RecordRoomMessageUseCase {
    private final MessageHistory history;

    public RecordRoomMessageUseCase(MessageHistory history) {
        this.history = history;
    }

    public StoredMessage record(String sender, String room, String text) {
        StoredMessage stored = new StoredMessage(LocalDateTime.now(), sender, room, text);
        history.append(stored);
        return stored;
    }
}
