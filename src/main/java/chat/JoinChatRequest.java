package chat;

import java.io.Serializable;

public class JoinChatRequest implements Serializable {
    private final String sessionId;

    public JoinChatRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
