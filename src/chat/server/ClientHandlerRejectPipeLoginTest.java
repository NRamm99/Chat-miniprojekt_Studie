package chat.server;

import chat.server.adapters.ChatRoomManagerImpl;
import chat.server.adapters.DefaultMessageParser;
import chat.server.adapters.InMemoryClientRegistry;
import chat.server.adapters.MessageDispatcherImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandlerRejectPipeLoginTest {
    public static void main(String[] args) throws Exception {
        ChatServer.REGISTERED_USERS.clear();
        ChatServer.ROOMS.clear();
        ChatServer.ROOMS.put(ChatServer.DEFAULT_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        InMemoryClientRegistry registry = new InMemoryClientRegistry(ChatServer.REGISTERED_USERS);
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(registry);
        ChatRoomManagerImpl roomManager = new ChatRoomManagerImpl();
        DefaultMessageParser parser = new DefaultMessageParser();
        ClientHandler handler = new ClientHandler(new Socket(), registry, dispatcher, roomManager, parser);

        invokeHandleLogin(handler, "Bo|b");

        String username = getFieldValue(handler, "username");

        if (username != null) {
            throw new AssertionError("Expected no accepted username, but was: " + username);
        }
        if (registry.get("Bo|b") != null) {
            throw new AssertionError("Expected registry not to contain username with '|'");
        }

        System.out.println("PASS: LOGIN with '|' is rejected and not registered.");
    }

    private static void invokeHandleLogin(ClientHandler handler, String username) throws Exception {
        Method method = ClientHandler.class.getDeclaredMethod("handleLogin", String.class);
        method.setAccessible(true);
        method.invoke(handler, username);
    }

    private static String getFieldValue(ClientHandler handler, String fieldName) throws Exception {
        Field field = ClientHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(handler);
    }
}
