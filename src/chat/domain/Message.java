package chat.domain;

public final class Message {
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_LOGIN = "LOGIN";

    private final String type;
    private final String text;

    private Message(String type, String text) {
        this.type = type == null ? TYPE_TEXT : type;
        this.text = text == null ? "" : text;
    }

    public static Message fromText(String text) {
        return new Message(TYPE_TEXT, text);
    }

    public static Message fromLogin(String username) {
        return new Message(TYPE_LOGIN, username);
    }

    public static Message fromProtocol(String rawMessage) {
        if (rawMessage == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        String[] parts = rawMessage.split("\\|", 3);
        if (parts.length >= 2 && TYPE_LOGIN.equals(parts[0])) {
            String username = parts.length == 3 ? parts[2] : "";
            return new Message(TYPE_LOGIN, username);
        }

        String messageText = rawMessage.startsWith(TYPE_TEXT + "||")
                ? rawMessage.substring((TYPE_TEXT + "||").length())
                : rawMessage;
        return new Message(TYPE_TEXT, messageText);
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String toProtocolString() {
        return type + "||" + text;
    }
}
