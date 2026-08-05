package server;

import model.Branch;
import model.Employee;
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

    @Test
    void getBranchesReturnsBothSeededBranches() {
        assertEquals(2, directory.getBranches().size());
    }

    @Test
    void findBranchByIdReturnsMatchingBranch() {
        Branch branch = directory.findBranchById("B1");

        assertNotNull(branch);
        assertEquals("Downtown", branch.getName());
    }

    @Test
    void findBranchByIdReturnsNullForUnknownId() {
        assertNull(directory.findBranchById("nope"));
    }

    @Test
    void addEmployeeMakesItFindableByUsername() {
        Branch branch = directory.findBranchById("B1");
        Employee newEmployee = new Employee("100000009", "Roi Biton", "050-9999999", "AC-9009", "E-009",
                "roi.b", "abcdef1", Role.SELLER, branch);

        directory.addEmployee(newEmployee);

        assertEquals(newEmployee, directory.findByUsername("roi.b"));
    }

    @Test
    void getAllEmployeesReturnsAllThreeSeededEmployees() {
        assertEquals(3, directory.getAllEmployees().size());
    }
}
