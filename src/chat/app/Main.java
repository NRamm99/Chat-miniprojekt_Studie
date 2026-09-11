package chat.app;

import java.util.Arrays;
import java.util.Locale;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("  java -cp out chat.server.ChatServer");
            System.out.println("  java -cp out chat.client.ChatClient");
            return;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (mode) {
            case "server":
                chat.server.ChatServer.main(remainingArgs);
                break;
            case "client":
                chat.client.ChatClient.main(remainingArgs);
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                System.out.println("Usage:");
                System.out.println("  java -cp out chat.server.ChatServer");
                System.out.println("  java -cp out chat.client.ChatClient");
                break;
        }
    }
}
