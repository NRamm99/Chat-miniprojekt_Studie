package chat.application;

import chat.application.ports.MessageHistory;
import chat.domain.StoredMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomHistoryUseCaseTest {
    @Test
    void recordAppendsMessageForRoom() {
        RecordingHistory history = new RecordingHistory();
        RecordRoomMessageUseCase useCase = new RecordRoomMessageUseCase(history);

        StoredMessage stored = useCase.record("Alice", "lobby", "Hej");

        assertEquals("Alice", stored.getSender());
        assertEquals("lobby", stored.getRoom());
        assertEquals("Hej", stored.getText());
        assertEquals(List.of(stored), history.recent("lobby"));
    }

    @Test
    void recentReturnsStoredMessagesForRoom() {
        RecordingHistory history = new RecordingHistory();
        StoredMessage lobby = new StoredMessage(LocalDateTime.of(2026, 9, 17, 22, 0), "Alice", "lobby", "Hej");
        history.append(lobby);
        GetRoomHistoryUseCase useCase = new GetRoomHistoryUseCase(history);

        assertEquals(List.of(lobby), useCase.recent("lobby"));
        assertTrue(useCase.recent("room67").isEmpty());
        assertTrue(useCase.recent(" ").isEmpty());
        assertTrue(useCase.recent(null).isEmpty());
    }

    private static final class RecordingHistory implements MessageHistory {
        private final Map<String, List<StoredMessage>> rooms = new LinkedHashMap<>();

        @Override
        public void append(StoredMessage message) {
            rooms.computeIfAbsent(message.getRoom(), key -> new ArrayList<>()).add(message);
        }

        @Override
        public List<StoredMessage> recent(String room) {
            return Collections.unmodifiableList(rooms.getOrDefault(room, List.of()));
        }
    }
}
