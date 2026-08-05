package chat;

import model.Employee;

public interface ChatParticipant {
    Employee getEmployee();

    void send(Object message);
}
