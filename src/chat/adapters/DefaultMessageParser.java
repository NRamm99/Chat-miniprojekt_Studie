package chat.adapters;

import chat.domain.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DefaultMessageParser implements MessageParser {
    public static final DateTimeFormatter SERVER_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Message parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Tom besked");
        }

        int separator = raw.indexOf('|');
        String type = separator < 0 ? raw : raw.substring(0, separator);

        switch (type) {
            case Message.TYPE_LOGIN:
                if (!raw.startsWith(Message.TYPE_LOGIN + "||")) {
                    throw new IllegalArgumentException("LOGIN kræver formatet LOGIN||<brugernavn>");
                }
                return Message.fromLogin(raw.substring((Message.TYPE_LOGIN + "||").length()));

            case Message.TYPE_TEXT:
                String[] textParts = raw.split("\\|", 3);
                if (textParts.length < 3) {
                    throw new IllegalArgumentException("Manglende påkrævede felter for TEXT");
                }
                return Message.fromText(textParts[1], textParts[2]);

            case Message.TYPE_JOIN_ROOM:
                String[] joinParts = raw.split("\\|", -1);
                if (joinParts.length != 3 || !joinParts[2].isEmpty()) {
                    throw new IllegalArgumentException("Manglende påkrævede felter for JOIN_ROOM");
                }
                return Message.fromJoinRoom(joinParts[1].trim());

            case Message.TYPE_PRIVATE:
                String[] privateParts = raw.split("\\|", 3);
                if (privateParts.length < 3) {
                    throw new IllegalArgumentException("Manglende påkrævede felter for PRIVATE");
                }
                return Message.fromPrivate(privateParts[1], privateParts[2]);

            case Message.TYPE_QUIT:
                if (!raw.equals(Message.TYPE_QUIT + "||")) {
                    throw new IllegalArgumentException("QUIT kræver formatet QUIT||");
                }
                return Message.fromQuit();

            default:
                throw new IllegalArgumentException("Ukendt meddelelsestype: " + type);
        }
    }

    @Override
    public String format(String type, String sender, String room, String text) {
        String messageType = type == null ? Message.TYPE_ERROR : type;
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

    @Override
    public String toProtocolString(Message message) {
        String type = message.getType();
        String room = message.getRoom();
        String text = message.getText();
        if (Message.TYPE_PRIVATE.equals(type)) {
            return Message.TYPE_PRIVATE + "|" + room + "|" + text;
        }
        if (Message.TYPE_TEXT.equals(type) && room != null && !room.isBlank()) {
            return type + "|" + room + "|" + text;
        }
        if (Message.TYPE_JOIN_ROOM.equals(type)) {
            return type + "|" + text + "|";
        }
        if (Message.TYPE_QUIT.equals(type)) {
            return Message.TYPE_QUIT + "||";
        }
        return type + "||" + text;
    }
}
