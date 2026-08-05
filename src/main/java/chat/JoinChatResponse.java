package chat;

import java.io.Serializable;

public class JoinChatResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;

    private JoinChatResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static JoinChatResponse success() {
        return new JoinChatResponse(true, null);
    }

    public static JoinChatResponse failure(String errorMessage) {
        return new JoinChatResponse(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
