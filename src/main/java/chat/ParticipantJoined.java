package chat;

import model.Employee;

import java.io.Serializable;

public class ParticipantJoined implements Serializable {
    private final String sessionId;
    private final Employee joinedEmployee;

    public ParticipantJoined(String sessionId, Employee joinedEmployee) {
        this.sessionId = sessionId;
        this.joinedEmployee = joinedEmployee;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Employee getJoinedEmployee() {
        return joinedEmployee;
    }
}
