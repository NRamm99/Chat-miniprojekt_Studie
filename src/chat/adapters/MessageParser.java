package chat.adapters;

import chat.domain.Message;

import java.time.LocalDateTime;

public interface MessageParser {
    Message parse(String raw);
    String format(String type, String sender, String room, String text);
    String format(String type, String sender, String room, String text, LocalDateTime timestamp);
    String toProtocolString(Message message);
}
