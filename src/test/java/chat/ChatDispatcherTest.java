package chat;

import model.Branch;
import model.Employee;
import model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatDispatcherTest {
    private final Branch branch1 = new Branch("B1", "Downtown", "1 Main St");
    private final Branch branch2 = new Branch("B2", "Uptown", "2 High St");

    private final ChatDispatcher dispatcher = ChatDispatcher.getInstance();

    @BeforeEach
    void resetDispatcher() {
        dispatcher.resetForTests();
    }

    private FakeChatParticipant participant(String username, Role role, Branch branch) {
        Employee employee = new Employee("1", username + "-full", username, "pw123456", role, branch);
        return new FakeChatParticipant(employee);
    }

    @Test
    void firstRequestWithNoOneWaitingGetsQueued() {
        FakeChatParticipant requester = participant("yossi", Role.CASHIER, branch1);

        ChatRequestOutcome outcome = dispatcher.requestChat(requester);

        assertInstanceOf(ChatQueued.class, outcome);
    }

    @Test
    void secondRequestFromDifferentBranchMatchesTheFirst() {
        FakeChatParticipant first = participant("yossi", Role.CASHIER, branch1);
        FakeChatParticipant second = participant("noa", Role.SELLER, branch2);

        dispatcher.requestChat(first);
        ChatRequestOutcome secondOutcome = dispatcher.requestChat(second);

        assertInstanceOf(ChatStarted.class, secondOutcome);
        ChatStarted started = (ChatStarted) secondOutcome;
        assertEquals(first.getEmployee(), started.getPeer());

        assertEquals(1, first.getReceived().size());
        ChatStarted pushedToFirst = (ChatStarted) first.getReceived().get(0);
        assertEquals(second.getEmployee(), pushedToFirst.getPeer());
        assertEquals(started.getSessionId(), pushedToFirst.getSessionId());
    }

    @Test
    void sameBranchRequestsDoNotMatchEachOther() {
        FakeChatParticipant first = participant("yossi", Role.CASHIER, branch1);
        FakeChatParticipant second = participant("dana", Role.MANAGER, branch1);

        dispatcher.requestChat(first);
        ChatRequestOutcome secondOutcome = dispatcher.requestChat(second);

        assertInstanceOf(ChatQueued.class, secondOutcome);
        assertTrue(first.getReceived().isEmpty());
    }

    @Test
    void waitingQueueIsFirstInFirstOut() {
        FakeChatParticipant first = participant("yossi", Role.CASHIER, branch1);
        FakeChatParticipant second = participant("moshe", Role.SELLER, branch1);
        FakeChatParticipant thirdFromOtherBranch = participant("noa", Role.SELLER, branch2);

        dispatcher.requestChat(first);
        dispatcher.requestChat(second);
        ChatRequestOutcome outcome = dispatcher.requestChat(thirdFromOtherBranch);

        ChatStarted started = (ChatStarted) outcome;
        assertEquals(first.getEmployee(), started.getPeer());
    }
}
