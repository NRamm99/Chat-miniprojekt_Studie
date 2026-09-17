package chat.domain;

public class MessagePipePreservationTest {
    public static void main(String[] args) {
        Message textMsg = Message.fromProtocol("TEXT|room1|Hej | verden");
        if (!"Hej | verden".equals(textMsg.getText())) {
            throw new AssertionError("TEXT message text was altered: " + textMsg.getText());
        }
        if (!"room1".equals(textMsg.getRoom())) {
            throw new AssertionError("TEXT message room mismatch: " + textMsg.getRoom());
        }

        Message privateMsg = Message.fromProtocol("PRIVATE|alice|Privat | besked");
        if (!"Privat | besked".equals(privateMsg.getText())) {
            throw new AssertionError("PRIVATE message text was altered: " + privateMsg.getText());
        }
        if (!"alice".equals(privateMsg.getRoom())) {
            throw new AssertionError("PRIVATE message recipient mismatch: " + privateMsg.getRoom());
        }

        System.out.println("PASS: '|' inside message texts is preserved for TEXT and PRIVATE.");
    }
}
