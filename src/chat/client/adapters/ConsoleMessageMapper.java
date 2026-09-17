package chat.client.adapters;

import chat.domain.Message;

public class ConsoleMessageMapper {
    public String map(String timestamp, String type, String sender, String room, String text) {
        String time = timestamp == null ? "" : timestamp;
        String payload = text == null ? "" : text;
        String from = sender == null ? "" : sender;
        String target = room == null ? "" : room;

        if (Message.TYPE_TEXT.equals(type)) {
            return withTime(time, "[" + target + "] " + from + ": " + payload);
        }
        if (Message.TYPE_PRIVATE.equals(type)) {
            return withTime(time, "[PRIVATE] " + from + ": " + payload);
        }
        if (Message.TYPE_ERROR.equals(type)) {
            return withTime(time, "Fejl: " + payload);
        }
        if (Message.TYPE_LOGIN.equals(type) || Message.TYPE_JOIN_ROOM.equals(type)) {
            return withTime(time, payload);
        }
        if (payload.isBlank()) {
            return withTime(time, type == null ? "" : type);
        }
        return withTime(time, payload);
    }

    public String mapRaw(String serverLine) {
        return serverLine == null ? "" : serverLine;
    }

    private static String withTime(String timestamp, String body) {
        if (timestamp.isBlank()) {
            return body;
        }
        return "[" + timestamp + "] " + body;
    }
}
