package chat.domain;

public final class Message {
    public static final String TYPE_PREFIX = "TEXT||";

    private final String text;

    private Message(String text) {
        this.text = text == null ? "" : text;
    }

    public static Message fromText(String text) {
        return new Message(text);
    }

    public static Message fromProtocol(String rawMessage) {
        if (rawMessage == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        String text = rawMessage.startsWith(TYPE_PREFIX)
                ? rawMessage.substring(TYPE_PREFIX.length())
                : rawMessage;
        return new Message(text);
    }

    public String getText() {
        return text;
    }

    public String toProtocolString() {
        return TYPE_PREFIX + text;
    }
}
