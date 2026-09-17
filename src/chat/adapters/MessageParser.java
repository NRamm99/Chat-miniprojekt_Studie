package chat.adapters;

import chat.domain.Message;

public interface MessageParser {
    Message parse(String raw);
    String format(String type, String sender, String room, String text);
    String toProtocolString(Message message);
}
