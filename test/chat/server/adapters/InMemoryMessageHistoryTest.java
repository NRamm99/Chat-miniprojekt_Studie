package chat.server.adapters;

import chat.domain.StoredMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryMessageHistoryTest {
    @Test
    void appendKeepsMessagesInOrder() {
        InMemoryMessageHistory history = new InMemoryMessageHistory();
        StoredMessage first = stored("lobby", "Alice", "Hej");
        StoredMessage second = stored("lobby", "Bob", "Hej igen");

        history.append(first);
        history.append(second);

        assertEquals(List.of(first, second), history.recent("lobby"));
    }

    @Test
    void appendDropsOldestWhenCapIsExceeded() {
        InMemoryMessageHistory history = new InMemoryMessageHistory();
        StoredMessage oldest = stored("lobby", "Alice", "0");
        history.append(oldest);
        for (int i = 1; i <= InMemoryMessageHistory.MAX_MESSAGES_PER_ROOM; i++) {
            history.append(stored("lobby", "Alice", String.valueOf(i)));
        }

        List<StoredMessage> recent = history.recent("lobby");
        assertEquals(InMemoryMessageHistory.MAX_MESSAGES_PER_ROOM, recent.size());
        assertEquals("1", recent.get(0).getText());
        assertEquals(String.valueOf(InMemoryMessageHistory.MAX_MESSAGES_PER_ROOM), recent.get(recent.size() - 1).getText());
    }

    @Test
    void recentIsIsolatedPerRoom() {
        InMemoryMessageHistory history = new InMemoryMessageHistory();
        StoredMessage lobbyMessage = stored("lobby", "Alice", "Hej lobby");
        StoredMessage roomMessage = stored("room67", "Bob", "Hej room67");

        history.append(lobbyMessage);
        history.append(roomMessage);

        assertEquals(List.of(lobbyMessage), history.recent("lobby"));
        assertEquals(List.of(roomMessage), history.recent("room67"));
        assertEquals(List.of(), history.recent("unknown"));
    }

    private static StoredMessage stored(String room, String sender, String text) {
        return new StoredMessage(LocalDateTime.of(2026, 9, 17, 22, 0), sender, room, text);
    }
}
