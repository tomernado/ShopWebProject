package chat;

import java.util.HashMap;
import java.util.LinkedList;
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
