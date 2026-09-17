package chat.application;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginUseCaseTest {
    private final LoginUseCase useCase = new LoginUseCase();

    @Test
    void rejectsTakenUsernameWithoutSeparateExistsCheck() {
        ConcurrentMap<String, String> users = new ConcurrentHashMap<>();
        users.put("Bob", "existing");
        Set<String> joined = new HashSet<>();

        ActionResult result = useCase.execute(
                null,
                "Bob",
                "lobby",
                username -> users.putIfAbsent(username, "new") == null,
                joined::add);

        assertFalse(result.isSuccess());
        assertEquals("Brugernavnet er optaget", result.getErrorMessage());
        assertEquals("existing", users.get("Bob"));
        assertTrue(joined.isEmpty());
    }

    @Test
    void rejectsSecondLoginOnSameConnection() {
        ConcurrentMap<String, String> users = new ConcurrentHashMap<>();
        Set<String> joined = new HashSet<>();

        ActionResult result = useCase.execute(
                "Alice",
                "Bob",
                "lobby",
                username -> users.putIfAbsent(username, "new") == null,
                joined::add);

        assertFalse(result.isSuccess());
        assertEquals("Denne forbindelse har allerede et accepteret brugernavn", result.getErrorMessage());
        assertTrue(users.isEmpty());
        assertTrue(joined.isEmpty());
    }

    @Test
    void registersAndJoinsDefaultRoomAtomically() {
        ConcurrentMap<String, String> users = new ConcurrentHashMap<>();
        Set<String> joined = new HashSet<>();

        ActionResult result = useCase.execute(
                null,
                "Alice",
                "lobby",
                username -> users.putIfAbsent(username, "new") == null,
                joined::add);

        assertTrue(result.isSuccess());
        assertEquals("Alice", result.getValue());
        assertEquals("new", users.get("Alice"));
        assertTrue(joined.contains("lobby"));
    }
}
