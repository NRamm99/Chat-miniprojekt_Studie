package chat.application;

import chat.application.ports.MessageHistory;
import chat.domain.StoredMessage;

import java.util.Collections;
import java.util.List;

public class GetRoomHistoryUseCase {
    private final MessageHistory history;

    public GetRoomHistoryUseCase(MessageHistory history) {
        this.history = history;
    }

    public List<StoredMessage> recent(String room) {
        if (room == null || room.isBlank()) {
            return Collections.emptyList();
        }
        return history.recent(room);
    }
}
