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

public class ChatClient {
    public static final String HOST = "localhost";
    public static final int PORT = 5001;
    public static final String DEFAULT_ROOM = "lobby";

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            AtomicBoolean loginAccepted = new AtomicBoolean(false);
            AtomicReference<CountDownLatch> loginLatchRef = new AtomicReference<>(new CountDownLatch(1));
            AtomicReference<String> currentRoomRef = new AtomicReference<>(DEFAULT_ROOM);

            Thread receiverThread = new Thread(() -> receiveServerMessages(in, loginLatchRef, loginAccepted, currentRoomRef), "chat-client-receiver");
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

               if (loginAccepted.get()) {
                   break;
               }
            }

            System.out.println("Connected to ChatServer at " + HOST + ":" + PORT + ". Current room: " + DEFAULT_ROOM + ". Type messages and press Enter to send. Ctrl+D (or Ctrl+Z then Enter on Windows) to exit.");
            System.out.println("\n--- Available Commands ---");
            System.out.println("/join <room>  - Switch to a different chat room (e.g., /join room67)");
            System.out.println("/msg <user> <text> - Send a private message to a specific user");
            System.out.println("/help         - Show this help message");
            System.out.println("--- End Commands ---\n");

            String line;
            while ((line = console.readLine()) != null) {
               if (line.startsWith("/join ")) {
                   String targetRoom = line.substring(6).trim();
                   if (!targetRoom.isEmpty()) {
                       out.println(Message.fromJoinRoom(targetRoom).toProtocolString());
                   } else {
                       System.out.println("Usage: /join <room>");
                   }
               } else if (line.startsWith("/msg ")) {
                   String rest = line.substring(5);
                   int idx = rest.indexOf(' ');
                   if (idx <= 0) {
                       System.out.println("Usage: /msg <user> <text>");
                   } else {
                       String recipient = rest.substring(0, idx).trim();
                       String text = rest.substring(idx + 1);
                       out.println(Message.fromPrivate(recipient, text).toProtocolString());
                   }
               } else if (line.equals("/help")) {
                   System.out.println("\n--- Available Commands ---");
                   System.out.println("/join <room>  - Switch to a different chat room (e.g., /join room67)");
                   System.out.println("/msg <user> <text> - Send a private message to a specific user");
                   System.out.println("/help         - Show this help message");
                   System.out.println("--- End Commands ---\n");
               } else if (!line.trim().isEmpty()) {
                   out.println(Message.fromText(currentRoomRef.get(), line).toProtocolString());
               }
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void receiveServerMessages(BufferedReader in,
                                             AtomicReference<CountDownLatch> loginLatchRef,
                                             AtomicBoolean loginAccepted,
                                             AtomicReference<String> currentRoomRef) {
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
                       System.out.println(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                       CountDownLatch latch = loginLatchRef.get();
                       if (latch != null) {
                           latch.countDown();
                       }
                       loginAccepted.set(false);
                       continue;
                   }

                   if (Message.TYPE_LOGIN.equals(type) && "server".equalsIgnoreCase(sender)) {
                       System.out.println(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                       loginAccepted.set(true);
                       CountDownLatch latch = loginLatchRef.get();
                       if (latch != null) {
                           latch.countDown();
                       }
                       continue;
                   }

                   if (Message.TYPE_JOIN_ROOM.equals(type) && "server".equalsIgnoreCase(sender)) {
                       System.out.println(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                       currentRoomRef.set(room);
                       continue;
                   }
n                   if (Message.TYPE_PRIVATE.equals(type)) {
                       // room is recipient; show clearly as private and show sender + text
                       System.out.println(chat.client.adapters.ClientPresenter.presentPrivate(sender, text));
                       continue;
                   }n                   if (Message.TYPE_TEXT.equals(type)) {
                       System.out.println(timestamp + "|" + type + "|" + sender + "|" + room + "|" + text);
                       continue;
                   }
               }
n               System.out.println(serverLine);
            }
        } catch (IOException e) {
            System.err.println("Server message error: " + e.getMessage());
        }
    }
}
