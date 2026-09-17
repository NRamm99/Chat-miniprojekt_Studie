package chat.adapters;

import chat.domain.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMessageParserTest {
    private final DefaultMessageParser parser = new DefaultMessageParser();

    @Test
    void parseValidClientMessages() {
        Message login = parser.parse("LOGIN||Alice");
        assertEquals(Message.TYPE_LOGIN, login.getType());
        assertEquals("Alice", login.getText());

        Message text = parser.parse("TEXT|lobby|Hej");
        assertEquals(Message.TYPE_TEXT, text.getType());
        assertEquals("lobby", text.getRoom());
        assertEquals("Hej", text.getText());

        Message join = parser.parse("JOIN_ROOM|room67|");
        assertEquals(Message.TYPE_JOIN_ROOM, join.getType());
        assertEquals("room67", join.getText());

        Message priv = parser.parse("PRIVATE|Bob|Hemmeligt");
        assertEquals(Message.TYPE_PRIVATE, priv.getType());
        assertEquals("Bob", priv.getRoom());
        assertEquals("Hemmeligt", priv.getText());

        Message quit = parser.parse("QUIT||");
        assertEquals(Message.TYPE_QUIT, quit.getType());
    }

    @Test
    void parseUnknownType() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parser.parse("UKENDT||Hej"));
        assertEquals("Ukendt meddelelsestype: UKENDT", error.getMessage());
    }

    @Test
    void parseMissingFields() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("TEXT"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("JOIN_ROOM|lobby"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("PRIVATE|Bob"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("LOGIN|Alice"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("QUIT|"));
    }

    @Test
    void parseValidEmptyFields() {
        Message emptyLogin = parser.parse("LOGIN||");
        assertEquals("", emptyLogin.getText());

        Message emptyText = parser.parse("TEXT|lobby|");
        assertEquals("lobby", emptyText.getRoom());
        assertEquals("", emptyText.getText());
    }

    @Test
    void parsePreservesPipeInPayload() {
        Message text = parser.parse("TEXT|lobby|a|b|c");
        assertEquals("a|b|c", text.getText());

        Message priv = parser.parse("PRIVATE|Bob|x|y");
        assertEquals("x|y", priv.getText());
    }

    @Test
    void formatServerAndClientLines() {
        assertEquals("LOGIN||Alice", parser.toProtocolString(Message.fromLogin("Alice")));
        assertEquals("TEXT|lobby|Hej", parser.toProtocolString(Message.fromText("lobby", "Hej")));
        assertEquals("JOIN_ROOM|room67|", parser.toProtocolString(Message.fromJoinRoom("room67")));
        assertEquals("PRIVATE|Bob|Hej", parser.toProtocolString(Message.fromPrivate("Bob", "Hej")));
        assertEquals("QUIT||", parser.toProtocolString(Message.fromQuit()));

        String formatted = parser.format("TEXT", "Alice", "lobby", "Hej|der");
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\|TEXT\\|Alice\\|lobby\\|Hej\\|der"));

        String emptyRoom = parser.format("LOGIN", "server", null, "ok");
        assertTrue(emptyRoom.contains("|LOGIN|server||ok"));
    }
}
