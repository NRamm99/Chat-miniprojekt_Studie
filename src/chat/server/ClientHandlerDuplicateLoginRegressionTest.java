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

public class ClientHandlerDuplicateLoginRegressionTest {
    public static void main(String[] args) throws Exception {
        ChatServer.REGISTERED_USERS.clear();
        ChatServer.ROOMS.clear();
        ChatServer.ROOMS.put(ChatServer.DEFAULT_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        ChatServer.ROOMS.put(ChatServer.SECOND_ROOM, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        InMemoryClientRegistry registry = new InMemoryClientRegistry(ChatServer.REGISTERED_USERS);
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(registry);
        ChatRoomManagerImpl roomManager = new ChatRoomManagerImpl();
        DefaultMessageParser parser = new DefaultMessageParser();
        ClientHandler handler = new ClientHandler(new Socket(), registry, dispatcher, roomManager, parser);

        invokeHandleLogin(handler, "Bob");
        invokeHandleJoinRoom(handler, ChatServer.SECOND_ROOM);
        invokeHandleLogin(handler, "Bob2");

        String username = getFieldValue(handler, "username");
        String room = getFieldValue(handler, "room");

        if (!"Bob".equals(username)) {
            throw new AssertionError("Expected username to remain Bob, but was: " + username);
        }
        if (!ChatServer.SECOND_ROOM.equals(room)) {
            throw new AssertionError("Expected room to remain room67, but was: " + room);
        }
        if (registry.get("Bob") != handler) {
            throw new AssertionError("Expected Bob to remain registered on this connection");
        }
        if (registry.get("Bob2") != null) {
            throw new AssertionError("Expected Bob2 not to be registered after duplicate LOGIN");
        }
        if (!roomManager.getMembers(ChatServer.SECOND_ROOM).contains(handler)) {
            throw new AssertionError("Expected connection to remain in room67");
        }
        if (roomManager.getMembers(ChatServer.DEFAULT_ROOM).contains(handler)) {
            throw new AssertionError("Expected connection not to remain in lobby after duplicate LOGIN rejection");
        }

        System.out.println("PASS: duplicate LOGIN after room switch is rejected without changing username or room state.");
    }

    private static void invokeHandleLogin(ClientHandler handler, String username) throws Exception {
        Method method = ClientHandler.class.getDeclaredMethod("handleLogin", String.class);
        method.setAccessible(true);
        method.invoke(handler, username);
    }

    private static void invokeHandleJoinRoom(ClientHandler handler, String room) throws Exception {
        Method method = ClientHandler.class.getDeclaredMethod("handleJoinRoom", String.class);
        method.setAccessible(true);
        method.invoke(handler, room);
    }

    private static String getFieldValue(ClientHandler handler, String fieldName) throws Exception {
        Field field = ClientHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(handler);
    }
}
