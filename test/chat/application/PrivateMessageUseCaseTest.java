package chat.application;

import chat.application.ports.MessageGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateMessageUseCaseTest {
    @Test
    void sendDeliversPrivateMessageWhenRecipientIsOnline() {
        RecordingGateway gateway = new RecordingGateway();
        PrivateMessageUseCase useCase = new PrivateMessageUseCase(Set.of("Bob")::contains, gateway);

        useCase.send("Alice", "Bob", "Hej");

        assertEquals(List.of("PRIVATE Alice->Bob: Hej"), gateway.events);
    }

    @Test
    void sendReportsErrorWhenRecipientIsUnknown() {
        RecordingGateway gateway = new RecordingGateway();
        PrivateMessageUseCase useCase = new PrivateMessageUseCase(username -> false, gateway);

        useCase.send("Alice", "Charlie", "Hej");

        assertEquals(List.of("ERROR Alice: Brugeren findes ikke: Charlie"), gateway.events);
    }

    @Test
    void sendReportsErrorWhenRecipientIsBlank() {
        RecordingGateway gateway = new RecordingGateway();
        PrivateMessageUseCase useCase = new PrivateMessageUseCase(new HashSet<String>()::contains, gateway);

        useCase.send("Alice", "  ", "Hej");

        assertEquals(List.of("ERROR Alice: Ugyldigt brugernavn for privat besked"), gateway.events);
        assertTrue(gateway.events.stream().noneMatch(event -> event.startsWith("PRIVATE")));
    }

    private static final class RecordingGateway implements MessageGateway {
        private final List<String> events = new ArrayList<>();

        @Override
        public void sendPrivate(String sender, String recipient, String text) {
            events.add("PRIVATE " + sender + "->" + recipient + ": " + text);
        }

        @Override
        public void sendErrorTo(String username, String text) {
            events.add("ERROR " + username + ": " + text);
        }
    }
}
