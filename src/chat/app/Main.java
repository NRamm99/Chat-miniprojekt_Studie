package chat.app;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

public final class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            LOG.info("Usage:");
            LOG.info("  java -cp out chat.server.ChatServer");
            LOG.info("  java -cp out chat.client.ChatClient");
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
                LOG.warning("Unknown mode: " + mode);
                LOG.info("Usage:");
                LOG.info("  java -cp out chat.server.ChatServer");
                LOG.info("  java -cp out chat.client.ChatClient");
                break;
        }
    }
}
