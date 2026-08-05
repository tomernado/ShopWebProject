package chat;

import java.io.Serializable;
import java.util.List;

public class ChatSummary implements Serializable {
    private final String sessionId;
    private final List<String> participantNames;

    public ChatSummary(String sessionId, List<String> participantNames) {
        this.sessionId = sessionId;
        this.participantNames = participantNames;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<String> getParticipantNames() {
        return participantNames;
    }
}
