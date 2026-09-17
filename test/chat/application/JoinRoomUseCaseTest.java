package chat.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinRoomUseCaseTest {
    private final JoinRoomUseCase useCase = new JoinRoomUseCase();

    @Test
    void joinsExistingRoomAndLeavesCurrent() {
        Set<String> rooms = Set.of("lobby", "room67");
        List<String> moves = new ArrayList<>();

        ActionResult result = useCase.execute(
                "lobby",
                "room67",
                rooms::contains,
                new RecordingMembership(moves));

        assertTrue(result.isSuccess());
        assertEquals("room67", result.getValue());
        assertEquals(List.of("leave:lobby", "join:room67"), moves);
    }

    @Test
    void rejectsUnknownRoomWithoutMembershipChange() {
        Set<String> rooms = new HashSet<>(Set.of("lobby", "room67"));
        List<String> moves = new ArrayList<>();

        ActionResult result = useCase.execute(
                "lobby",
                "nonexistent",
                rooms::contains,
                new RecordingMembership(moves));

        assertFalse(result.isSuccess());
        assertEquals("Rummet findes ikke", result.getErrorMessage());
        assertTrue(moves.isEmpty());
    }

    private static final class RecordingMembership implements chat.application.ports.RoomMembership {
        private final List<String> moves;

        private RecordingMembership(List<String> moves) {
            this.moves = moves;
        }

        @Override
        public void join(String room) {
            moves.add("join:" + room);
        }

        @Override
        public void leave(String room) {
            moves.add("leave:" + room);
        }
    }
}
