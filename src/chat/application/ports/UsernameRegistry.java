package chat.application.ports;

public interface UsernameRegistry {
    boolean tryRegister(String username);
}
