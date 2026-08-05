package chat;

import model.Employee;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private final String sessionId;
    private final Employee sender;
    private final String text;

    public ChatMessage(String sessionId, Employee sender, String text) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.text = text;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Employee getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }
}
