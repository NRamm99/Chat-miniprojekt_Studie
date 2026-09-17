package chat.server.adapters;

import chat.domain.Message;

public class DefaultMessageParser implements MessageParser {
    @Override
    public Message parse(String raw) {
        return Message.fromProtocol(raw);
    }

    @Override
    public String format(String type, String sender, String room, String text) {
        return Message.formatServerMessage(type, sender, room, text);
    }
}
