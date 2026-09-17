package chat.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class StoredMessage {
    private final LocalDateTime timestamp;
    private final String sender;
    private final String room;
    private final String text;

    public StoredMessage(LocalDateTime timestamp, String sender, String room, String text) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.sender = sender == null ? "" : sender;
        this.room = room == null ? "" : room;
        this.text = text == null ? "" : text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getRoom() {
        return room;
    }

    public String getText() {
        return text;
    }
}
