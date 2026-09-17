package chat.application.ports;

import chat.domain.StoredMessage;

import java.util.List;

public interface MessageHistory {
    void append(StoredMessage message);

    List<StoredMessage> recent(String room);
}
