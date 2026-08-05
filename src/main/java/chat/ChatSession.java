package chat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {
    private final String sessionId = UUID.randomUUID().toString();
    private final List<ChatParticipant> participants = new ArrayList<>();

    public ChatSession(ChatParticipant first, ChatParticipant second) {
        participants.add(first);
        participants.add(second);
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<ChatParticipant> getParticipants() {
        return participants;
    }

    // Not synchronized on its own — always called from within a synchronized
    // ChatDispatcher method, which is the sole owner of session mutation.
    public void addParticipant(ChatParticipant participant) {
        participants.add(participant);
    }

    public void broadcast(Object message, ChatParticipant sender) {
        for (ChatParticipant participant : participants) {
            if (participant != sender) {
                participant.send(message);
            }
        }
    }
}
