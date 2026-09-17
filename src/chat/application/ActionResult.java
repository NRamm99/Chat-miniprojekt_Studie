package chat.application;

public final class ActionResult {
    private final boolean success;
    private final String errorMessage;
    private final String value;

    private ActionResult(boolean success, String errorMessage, String value) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.value = value;
    }

    public static ActionResult ok(String value) {
        return new ActionResult(true, null, value);
    }

    public static ActionResult error(String errorMessage) {
        return new ActionResult(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getValue() {
        return value;
    }
}
