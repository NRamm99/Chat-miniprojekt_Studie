package chat.application;

import chat.application.ports.RoomCatalog;
import chat.application.ports.RoomMembership;

public class JoinRoomUseCase {
    public ActionResult execute(String currentRoom,
                                String targetRoom,
                                RoomCatalog catalog,
                                RoomMembership membership) {
        if (targetRoom == null || targetRoom.isBlank()) {
            return ActionResult.error("Rumnavnet kan ikke være tomt");
        }

        if (currentRoom != null && currentRoom.equals(targetRoom)) {
            return ActionResult.error("Du er allerede i det rum");
        }

        if (!catalog.exists(targetRoom)) {
            return ActionResult.error("Rummet findes ikke");
        }

        if (currentRoom != null) {
            membership.leave(currentRoom);
        }
        membership.join(targetRoom);
        return ActionResult.ok(targetRoom);
    }
}
