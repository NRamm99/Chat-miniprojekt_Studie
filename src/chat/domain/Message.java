package chat.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Message {
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_JOIN_ROOM = "JOIN_ROOM";
    public static final String TYPE_PRIVATE = "PRIVATE";
    public static final String TYPE_QUIT = "QUIT";
    public static final DateTimeFormatter SERVER_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
       if (rawMessage.isBlank()) {
           throw new IllegalArgumentException("Tom besked");
       }

       int separator = rawMessage.indexOf('|');
       String type = separator < 0 ? rawMessage : rawMessage.substring(0, separator);

       switch (type) {
           case TYPE_LOGIN:
               if (!rawMessage.startsWith(TYPE_LOGIN + "||")) {
                   throw new IllegalArgumentException("LOGIN kræver formatet LOGIN||<brugernavn>");
               }
               return new Message(TYPE_LOGIN, rawMessage.substring((TYPE_LOGIN + "||").length()));

           case TYPE_TEXT:
               String[] textParts = rawMessage.split("\\|", 3);
               if (textParts.length < 3) {
                   throw new IllegalArgumentException("Manglende påkrævede felter for TEXT");
               }
               return new Message(TYPE_TEXT, textParts[2], textParts[1]);

           case TYPE_JOIN_ROOM:
               String[] joinParts = rawMessage.split("\\|", -1);
               if (joinParts.length != 3 || !joinParts[2].isEmpty()) {
                   throw new IllegalArgumentException("Manglende påkrævede felter for JOIN_ROOM");
               }
               return new Message(TYPE_JOIN_ROOM, joinParts[1].trim());

           case TYPE_PRIVATE:
               String[] privateParts = rawMessage.split("\\|", 3);
               if (privateParts.length < 3) {
                   throw new IllegalArgumentException("Manglende påkrævede felter for PRIVATE");
               }
               return new Message(TYPE_PRIVATE, privateParts[2], privateParts[1]);

           case TYPE_QUIT:
               if (!rawMessage.equals(TYPE_QUIT + "||")) {
                   throw new IllegalArgumentException("QUIT kræver formatet QUIT||");
               }
               return fromQuit();

           default:
               throw new IllegalArgumentException("Ukendt meddelelsestype: " + type);
       }
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
       if (TYPE_PRIVATE.equals(type)) {
           return TYPE_PRIVATE + "|" + room + "|" + text;
       }
       if (TYPE_TEXT.equals(type) && room != null && !room.isBlank()) {
           return type + "|" + room + "|" + text;
       }
       if (TYPE_JOIN_ROOM.equals(type)) {
           return type + "|" + text + "|";
       }
       if (TYPE_QUIT.equals(type)) {
           return TYPE_QUIT + "||";
       }
       return type + "||" + text;
    }
}
