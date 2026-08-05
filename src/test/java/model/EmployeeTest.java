package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeTest {
    @Test
    void constructorStoresAllFields() {
        Branch branch = new Branch("B1", "Downtown", "1 Main St");
        Employee employee = new Employee(
                "123456789", "Dana Levi", "050-1234567", "AC-1001", "E-001",
                "dana.l", "secret123", Role.CASHIER, branch);

        assertEquals("123456789", employee.getIdNumber());
        assertEquals("Dana Levi", employee.getFullName());
        assertEquals("050-1234567", employee.getPhone());
        assertEquals("AC-1001", employee.getAccountNumber());
        assertEquals("E-001", employee.getEmployeeNumber());
        assertEquals("dana.l", employee.getUsername());
        assertEquals("secret123", employee.getPassword());
        assertEquals(Role.CASHIER, employee.getRole());
        assertEquals(branch, employee.getBranch());
    }
}
