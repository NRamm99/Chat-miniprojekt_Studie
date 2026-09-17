package chat.client.adapters;

public class ClientPresenter {
    private static final ConsoleMessageMapper MAPPER = new ConsoleMessageMapper();

    public static String presentPrivate(String sender, String text) {
        return MAPPER.map("", "PRIVATE", sender, "", text);
    }
}
