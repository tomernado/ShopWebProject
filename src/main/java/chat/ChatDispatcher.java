package chat;

import model.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ChatDispatcher {
    private static final ChatDispatcher INSTANCE = new ChatDispatcher();

    private final Queue<ChatParticipant> waitingQueue = new LinkedList<>();
    private final Map<String, ChatSession> sessionsById = new HashMap<>();

    private ChatDispatcher() {
    }

    public static ChatDispatcher getInstance() {
        return INSTANCE;
    }

    public synchronized ChatRequestOutcome requestChat(ChatParticipant requester) {
        ChatParticipant partner = findWaitingPartnerFromDifferentBranch(requester);

        if (partner == null) {
            waitingQueue.add(requester);
            return new ChatQueued();
        }

        ChatSession session = new ChatSession(requester, partner);
        sessionsById.put(session.getSessionId(), session);

        partner.send(new ChatStarted(session.getSessionId(), requester.getEmployee()));
        return new ChatStarted(session.getSessionId(), partner.getEmployee());
    }

    public synchronized JoinChatResponse joinChat(String sessionId, ChatParticipant participant) {
        if (participant.getEmployee().getRole() != Role.MANAGER) {
            return JoinChatResponse.failure("Only a shift manager can join an existing chat");
        }

        ChatSession session = sessionsById.get(sessionId);
        if (session == null) {
            return JoinChatResponse.failure("Chat session not found");
        }

        session.addParticipant(participant);
        session.broadcast(new ParticipantJoined(sessionId, participant.getEmployee()), participant);
        return JoinChatResponse.success();
    }

    public synchronized void sendMessage(String sessionId, ChatParticipant sender, String text) {
        ChatSession session = sessionsById.get(sessionId);
        if (session == null) {
            return;
        }
        session.broadcast(new ChatMessage(sessionId, sender.getEmployee(), text), sender);
    }

    public synchronized List<ChatSummary> listActiveChats() {
        List<ChatSummary> summaries = new ArrayList<>();
        for (ChatSession session : sessionsById.values()) {
            List<String> names = new ArrayList<>();
            for (ChatParticipant participant : session.getParticipants()) {
                names.add(participant.getEmployee().getFullName());
            }
            summaries.add(new ChatSummary(session.getSessionId(), names));
        }
        return summaries;
    }

    private ChatParticipant findWaitingPartnerFromDifferentBranch(ChatParticipant requester) {
        String requesterBranchId = requester.getEmployee().getBranch().getBranchId();
        for (ChatParticipant candidate : waitingQueue) {
            if (!candidate.getEmployee().getBranch().getBranchId().equals(requesterBranchId)) {
                waitingQueue.remove(candidate);
                return candidate;
            }
        }
        return null;
    }

    // Test-only: clears all state so each test starts from a clean dispatcher.
    public synchronized void resetForTests() {
        waitingQueue.clear();
        sessionsById.clear();
    }
}
