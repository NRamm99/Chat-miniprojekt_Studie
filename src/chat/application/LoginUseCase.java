package chat.application;

import chat.application.ports.RoomJoiner;
import chat.application.ports.UsernameRegistry;

public class LoginUseCase {
    public ActionResult execute(String currentUsername,
                                String requestedUsername,
                                String defaultRoom,
                                UsernameRegistry usernames,
                                RoomJoiner rooms) {
        if (currentUsername != null) {
            return ActionResult.error("Denne forbindelse har allerede et accepteret brugernavn");
        }

        if (requestedUsername == null) {
            return ActionResult.error("Brugernavnet kan ikke være tomt");
        }

        String normalizedUsername = requestedUsername.trim();
        if (normalizedUsername.isEmpty()) {
            return ActionResult.error("Brugernavnet kan ikke være tomt");
        }

        if (normalizedUsername.indexOf('|') >= 0) {
            return ActionResult.error("Ugyldigt brugernavn: indeholder forbudt tegn '|' ");
        }

        if (!usernames.tryRegister(normalizedUsername)) {
            return ActionResult.error("Brugernavnet er optaget");
        }

        rooms.join(defaultRoom);
        return ActionResult.ok(normalizedUsername);
    }
}
