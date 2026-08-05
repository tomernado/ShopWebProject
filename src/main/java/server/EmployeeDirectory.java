package server;

import model.Branch;
import model.Employee;
import model.Role;

import java.util.HashMap;
import java.util.Map;

public class EmployeeDirectory {
    private final Map<String, Employee> employeesByUsername = new HashMap<>();

    public EmployeeDirectory() {
        Branch downtown = new Branch("B1", "Downtown", "1 Main St");
        Branch uptown = new Branch("B2", "Uptown", "2 High St");

        addEmployee(new Employee("100000001", "Dana Levi", "dana.l", "secret123", Role.MANAGER, downtown));
        addEmployee(new Employee("100000002", "Yossi Cohen", "yossi.c", "pass456", Role.CASHIER, downtown));
        addEmployee(new Employee("100000003", "Noa Biton", "noa.b", "qwerty789", Role.SELLER, uptown));
    }

    private void addEmployee(Employee employee) {
        employeesByUsername.put(employee.getUsername(), employee);
    }

    public Employee findByUsername(String username) {
        return employeesByUsername.get(username);
    }
}
