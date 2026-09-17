package chat.client;

import chat.domain.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class ChatClient {
    private static final Logger LOG = Logger.getLogger(ChatClient.class.getName());
    public static final String HOST = "localhost";
    public static final int PORT = 5001;
    public static final String DEFAULT_ROOM = "lobby";

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            AtomicBoolean loginAccepted = new AtomicBoolean(false);
            AtomicBoolean serverConnectionClosed = new AtomicBoolean(false);
            AtomicBoolean clientClosing = new AtomicBoolean(false);
            AtomicReference<CountDownLatch> loginLatchRef = new AtomicReference<>(new CountDownLatch(1));
            AtomicReference<String> currentRoomRef = new AtomicReference<>(DEFAULT_ROOM);

            Thread receiverThread = new Thread(() -> receiveServerMessages(
                    in, loginLatchRef, loginAccepted, currentRoomRef, serverConnectionClosed, clientClosing),
                    "chat-client-receiver");
            receiverThread.start();

            while (true) {
               System.out.print("Choose a username: ");
               String username = console.readLine();
               if (username == null) {
                   return;
               }

               username = username.trim();
               if (username.isEmpty()) {
                   continue;
               }

               loginAccepted.set(false);
               CountDownLatch nextLoginLatch = new CountDownLatch(1);
               loginLatchRef.set(nextLoginLatch);

               out.println(Message.fromLogin(username).toProtocolString());
               try {
                   nextLoginLatch.await();
               } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
                   return;
               }

               if (serverConnectionClosed.get()) {
                   return;
               }

               if (loginAccepted.get()) {
                   break;
               }
            }

            LOG.info("Connected to ChatServer at " + HOST + ":" + PORT + ". Current room: " + DEFAULT_ROOM + ". Type messages and press Enter to send. Ctrl+D (or Ctrl+Z then Enter on Windows) to exit.");
            LOG.info("\n--- Available Commands ---");
            LOG.info("/join <room>  - Switch to a different chat room (e.g., /join room67)");
            LOG.info("/msg <user> <text> - Send a private message to a specific user");
            LOG.info("/quit         - Close the connection and exit");
            LOG.info("/help         - Show this help message");
            LOG.info("--- End Commands ---\n");

            String line;
            while ((line = console.readLine()) != null) {
               if (line.equals("/quit")) {
                   clientClosing.set(true);
                   out.println(Message.fromQuit().toProtocolString());
                   out.flush();
                   break;
               } else if (line.startsWith("/join ")) {
                   String targetRoom = line.substring(6).trim();
                   if (!targetRoom.isEmpty()) {
                       out.println(Message.fromJoinRoom(targetRoom).toProtocolString());
                   } else {
                       LOG.info("Usage: /join <room>");
                   }
               } else if (line.startsWith("/msg ")) {
                   String rest = line.substring(5);
                   int idx = rest.indexOf(' ');
                   if (idx <= 0) {
                       LOG.info("Usage: /msg <user> <text>");
                   } else {
                       String recipient = rest.substring(0, idx).trim();
                       String text = rest.substring(idx + 1);
                       out.println(Message.fromPrivate(recipient, text).toProtocolString());
                   }
               } else if (line.equals("/help")) {
                   LOG.info("\n--- Available Commands ---");
                   LOG.info("/join <room>  - Switch to a different chat room (e.g., /join room67)");
                   LOG.info("/msg <user> <text> - Send a private message to a specific user");
                   LOG.info("/quit         - Close the connection and exit");
                   LOG.info("/help         - Show this help message");
                   LOG.info("--- End Commands ---\n");
               } else if (!line.trim().isEmpty()) {
                   out.println(Message.fromText(currentRoomRef.get(), line).toProtocolString());
               }
            }

        } catch (IOException e) {
            LOG.severe("Client error: " + e.getMessage());
        }
    }

    private static void receiveServerMessages(BufferedReader in,
                                             AtomicReference<CountDownLatch> loginLatchRef,
                                             AtomicBoolean loginAccepted,
                                             AtomicReference<String> currentRoomRef,
                                             AtomicBoolean serverConnectionClosed,
                                             AtomicBoolean clientClosing) {
        try {
            String serverLine;
            while ((serverLine = in.readLine()) != null) {
                String[] parts = serverLine.split("\\|", 5);
                if (parts.length >= 4) {
                    String timestamp = parts[0];
                    String type = parts[1];
                    String sender = parts[2];
                    String room = parts.length > 3 ? parts[3] : "";
                    String text = parts.length == 5 ? parts[4] : "";

                    if (Message.TYPE_ERROR.equals(type)) {
                        LOG.info(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                        CountDownLatch latch = loginLatchRef.get();
                        if (latch != null) {
                            latch.countDown();
                        }
                        loginAccepted.set(false);
                        continue;
                    }

                    if (Message.TYPE_LOGIN.equals(type) && "server".equalsIgnoreCase(sender)) {
                        LOG.info(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                        loginAccepted.set(true);
                        CountDownLatch latch = loginLatchRef.get();
                        if (latch != null) {
                            latch.countDown();
                        }
                        continue;
                    }

                    if (Message.TYPE_JOIN_ROOM.equals(type) && "server".equalsIgnoreCase(sender)) {
                        LOG.info(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                        currentRoomRef.set(room);
                        continue;
                    }

                    if (Message.TYPE_PRIVATE.equals(type)) {
                        // room is recipient; show clearly as private and show sender + text
                        LOG.info(chat.client.adapters.ClientPresenter.presentPrivate(sender, text));
                        continue;
                    }

                    if (Message.TYPE_TEXT.equals(type)) {
                        LOG.info(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                        continue;
                    }
                }

                LOG.info(serverLine);
            }
            if (!clientClosing.get()) {
                LOG.warning("Forbindelsen til serveren blev lukket.");
            }
        } catch (IOException e) {
            if (!clientClosing.get()) {
                LOG.warning("Forbindelsen til serveren blev afbrudt: " + e.getMessage());
            }
        } finally {
            serverConnectionClosed.set(true);
            CountDownLatch latch = loginLatchRef.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
