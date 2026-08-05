package chat;

import model.Employee;

import java.util.ArrayList;
import java.util.List;

class FakeChatParticipant implements ChatParticipant {
    private final Employee employee;
    private final List<Object> received = new ArrayList<>();

    FakeChatParticipant(Employee employee) {
        this.employee = employee;
    }

    @Override
    public Employee getEmployee() {
        return employee;
    }

    @Override
    public void send(Object message) {
        received.add(message);
    }

    List<Object> getReceived() {
        return received;
    }
}
