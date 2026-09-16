package chat.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Message {
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_JOIN_ROOM = "JOIN_ROOM";
    public static final DateTimeFormatter SERVER_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String type;
    private final String text;
    private final String room;

    private Message(String type, String text) {
       this(type, text, "");
    }

    private Message(String type, String text, String room) {
        this.type = type == null ? TYPE_TEXT : type;
        this.text = text == null ? "" : text;
        this.room = room == null ? "" : room;
    }

    public static Message fromText(String text) {
       return fromText("", text);
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

    public static String formatServerMessage(String type, String sender, String text) {
       return formatServerMessage(type, sender, "", text);
    }

    public static String formatServerMessage(String type, String sender, String room, String text) {
       String messageType = type == null ? TYPE_ERROR : type;
       String messageSender = sender == null ? "server" : sender;
       String roomValue = room == null ? "" : room;
       String payload = (text == null ? "" : text);
       String roomPart = roomValue.isBlank() ? "||" : "|" + roomValue + "|";
       return LocalDateTime.now().format(SERVER_TIMESTAMP_FORMAT)
               + "|" + messageType
               + "|" + messageSender
               + roomPart
               + payload;
    }


    public static Message fromProtocol(String rawMessage) {
       if (rawMessage == null) {
           throw new IllegalArgumentException("Message cannot be null");
       }

       if (rawMessage.startsWith(TYPE_LOGIN + "||")) {
           String username = rawMessage.substring((TYPE_LOGIN + "||").length());
           return new Message(TYPE_LOGIN, username);
       }

       if (rawMessage.startsWith(TYPE_TEXT + "||")) {
           String text = rawMessage.substring((TYPE_TEXT + "||").length());
           return new Message(TYPE_TEXT, text, "");
       }

       if (rawMessage.startsWith(TYPE_JOIN_ROOM + "|")) {
           String[] parts = rawMessage.split("\\|", 2);
           String room = parts.length >= 2 ? parts[1] : "";
           return new Message(TYPE_JOIN_ROOM, room);
       }

       String[] parts = rawMessage.split("\\|", 3);
       if (parts.length >= 2 && TYPE_TEXT.equals(parts[0])) {
           String room = parts.length >= 2 ? parts[1] : "";
           String messageText = parts.length == 3 ? parts[2] : "";
           return new Message(TYPE_TEXT, messageText, room);
       }

       return new Message(TYPE_TEXT, rawMessage, "");
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

    public String toProtocolString() {
       if (TYPE_TEXT.equals(type) && room != null && !room.isBlank()) {
           return type + "|" + room + "|" + text;
       }
       if (TYPE_JOIN_ROOM.equals(type)) {
           return type + "|" + text + "|";
       }
       return type + "||" + text;
    }
}
