package server;

import java.io.Serializable;
import java.util.List;

public class GetLogsResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;
    private final List<String> lines;

    private GetLogsResponse(boolean success, String errorMessage, List<String> lines) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.lines = lines;
    }

    public static GetLogsResponse success(List<String> lines) {
        return new GetLogsResponse(true, null, lines);
    }

    public static GetLogsResponse failure(String errorMessage) {
        return new GetLogsResponse(false, errorMessage, List.of());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getLines() {
        return lines;
    }
}
