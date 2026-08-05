package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeTest {
    @Test
    void constructorStoresAllFields() {
        Branch branch = new Branch("B1", "Downtown", "1 Main St");
        Employee employee = new Employee("123456789", "Dana Levi", "dana.l", Role.CASHIER, branch);

        assertEquals("123456789", employee.getIdNumber());
        assertEquals("Dana Levi", employee.getFullName());
        assertEquals("dana.l", employee.getUsername());
        assertEquals(Role.CASHIER, employee.getRole());
        assertEquals(branch, employee.getBranch());
    }
}
