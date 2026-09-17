package chat.server.adapters;

import chat.application.ports.MessageHistory;
import chat.domain.StoredMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryMessageHistory implements MessageHistory {
    public static final int MAX_MESSAGES_PER_ROOM = 20;

    private final ConcurrentMap<String, Deque<StoredMessage>> rooms = new ConcurrentHashMap<>();

    @Override
    public void append(StoredMessage message) {
        if (message == null || message.getRoom() == null || message.getRoom().isBlank()) {
            return;
        }
        Deque<StoredMessage> queue = rooms.computeIfAbsent(message.getRoom(), key -> new ArrayDeque<>());
        synchronized (queue) {
            while (queue.size() >= MAX_MESSAGES_PER_ROOM) {
                queue.removeFirst();
            }
            queue.addLast(message);
        }
    }

    @Override
    public List<StoredMessage> recent(String room) {
        if (room == null || room.isBlank()) {
            return Collections.emptyList();
        }
        Deque<StoredMessage> queue = rooms.get(room);
        if (queue == null) {
            return Collections.emptyList();
        }
        synchronized (queue) {
            return Collections.unmodifiableList(new ArrayList<>(queue));
        }
    }
}
