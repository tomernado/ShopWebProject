package chat;

import model.Employee;

public class ChatStarted implements ChatRequestOutcome {
    private final String sessionId;
    private final Employee peer;

    public ChatStarted(String sessionId, Employee peer) {
        this.sessionId = sessionId;
        this.peer = peer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Employee getPeer() {
        return peer;
    }
}
