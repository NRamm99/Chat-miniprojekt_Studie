package chat.domain;

public final class Message {
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_JOIN_ROOM = "JOIN_ROOM";
    public static final String TYPE_PRIVATE = "PRIVATE";
    public static final String TYPE_QUIT = "QUIT";

    private final String type;
    private final String text;
    private final String room; // for TEXT: room; for PRIVATE: recipient username

    private Message(String type, String text) {
       this(type, text, "");
    }

    private Message(String type, String text, String room) {
        this.type = type == null ? TYPE_TEXT : type;
        this.text = text == null ? "" : text;
        this.room = room == null ? "" : room;
    }

    public static Message fromText(String room, String text) {
       return new Message(TYPE_TEXT, text, room);
    }

    public static Message fromLogin(String username) {
       return new Message(TYPE_LOGIN, username);
    }

    public static Message fromJoinRoom(String room) {
       return new Message(TYPE_JOIN_ROOM, room);
    }

    public static Message fromPrivate(String recipient, String text) {
       return new Message(TYPE_PRIVATE, text, recipient);
    }

    public static Message fromQuit() {
       return new Message(TYPE_QUIT, "");
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getRoom() {
        return room;
    }
}
