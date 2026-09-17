package chat.server.adapters;

import chat.domain.Message;

public interface MessageParser {
    Message parse(String raw);
    String format(String type, String sender, String room, String text);
}
