package chat.client.adapters;

import chat.domain.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleMessageMapperTest {
    private final ConsoleMessageMapper mapper = new ConsoleMessageMapper();

    @Test
    void mapsTextWithoutProtocolPipes() {
        String line = mapper.map("2026-09-17 22:41:05", Message.TYPE_TEXT, "Alice", "lobby", "Hej|der");
        assertEquals("[2026-09-17 22:41:05] [lobby] Alice: Hej|der", line);
    }

    @Test
    void mapsPrivateLoginJoinAndError() {
        assertEquals("[2026-09-17 22:41:05] [PRIVATE] Alice: Hemmeligt",
                mapper.map("2026-09-17 22:41:05", Message.TYPE_PRIVATE, "Alice", "Bob", "Hemmeligt"));
        assertEquals("[2026-09-17 22:41:05] Brugernavnet er accepteret: Alice",
                mapper.map("2026-09-17 22:41:05", Message.TYPE_LOGIN, "server", "", "Brugernavnet er accepteret: Alice"));
        assertEquals("[2026-09-17 22:41:05] Du er nu i rum room67",
                mapper.map("2026-09-17 22:41:05", Message.TYPE_JOIN_ROOM, "server", "room67", "Du er nu i rum room67"));
        assertEquals("[2026-09-17 22:41:05] Fejl: Brugernavnet er optaget",
                mapper.map("2026-09-17 22:41:05", Message.TYPE_ERROR, "server", "", "Brugernavnet er optaget"));
    }

    @Test
    void presentPrivateMatchesMappedPrivateWithoutTimestamp() {
        assertEquals("[PRIVATE] Alice: Hej", ClientPresenter.presentPrivate("Alice", "Hej"));
    }
}
