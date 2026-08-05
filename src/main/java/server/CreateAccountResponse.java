package server;

import java.io.Serializable;

public class CreateAccountResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;

    private CreateAccountResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static CreateAccountResponse success() {
        return new CreateAccountResponse(true, null);
    }

    public static CreateAccountResponse failure(String errorMessage) {
        return new CreateAccountResponse(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
