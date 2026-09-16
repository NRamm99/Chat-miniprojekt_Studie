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

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            AtomicBoolean loginAccepted = new AtomicBoolean(false);
            AtomicReference<CountDownLatch> loginLatchRef = new AtomicReference<>(new CountDownLatch(1));

            Thread receiverThread = new Thread(() -> receiveServerMessages(in, loginLatchRef, loginAccepted), "chat-client-receiver");
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

            System.out.println("Connected to ChatServer at " + HOST + ":" + PORT + ". Type messages and press Enter to send. Ctrl+D (or Ctrl+Z then Enter on Windows) to exit.");

            String line;
            while ((line = console.readLine()) != null) {
                out.println(Message.fromText(line).toProtocolString());
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void receiveServerMessages(BufferedReader in,
                                             AtomicReference<CountDownLatch> loginLatchRef,
                                             AtomicBoolean loginAccepted) {
        try {
            String serverLine;
            while ((serverLine = in.readLine()) != null) {
                String[] parts = serverLine.split("\\|", 5);
                if (parts.length >= 4) {
                    String timestamp = parts[0];
                    String type = parts[1];
                    String sender = parts[2];
                    String text = parts.length == 5 ? parts[4] : "";
                    String formatted = timestamp + "|" + type + "|" + sender + "||" + text;

                    if (Message.TYPE_ERROR.equals(type)) {
                        System.out.println(formatted);
                        CountDownLatch latch = loginLatchRef.get();
                        if (latch != null) {
                            latch.countDown();
                        }
                        loginAccepted.set(false);
                        continue;
                    }

                    if (Message.TYPE_LOGIN.equals(type) && "server".equalsIgnoreCase(sender)) {
                        System.out.println(formatted);
                        loginAccepted.set(true);
                        CountDownLatch latch = loginLatchRef.get();
                        if (latch != null) {
                            latch.countDown();
                        }
                        continue;
                    }

                    if (Message.TYPE_TEXT.equals(type)) {
                        System.out.println(formatted);
                        continue;
                    }
                }

                System.out.println(serverLine);
            }
        } catch (IOException e) {
            System.err.println("Server message error: " + e.getMessage());
        }
    }
}
