package server;

import model.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeDirectoryTest {
    private final EmployeeDirectory directory = new EmployeeDirectory();

    @Test
    void findsSeededEmployeeByUsername() {
        var employee = directory.findByUsername("dana.l");

        assertNotNull(employee);
        assertEquals("Dana Levi", employee.getFullName());
        assertEquals(Role.MANAGER, employee.getRole());
    }

    @Test
    void unknownUsernameReturnsNull() {
        assertNull(directory.findByUsername("nobody"));
    }
}
