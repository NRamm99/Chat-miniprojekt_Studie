package chat.client.adapters;

public class ClientPresenter {
    public static String presentPrivate(String sender, String text) {
        return "[PRIVATE] " + sender + ": " + text;
    }
}
