package server;

import model.Branch;
import model.Employee;
import model.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeDirectory {
    private final Map<String, Employee> employeesByUsername = new HashMap<>();
    private final Map<String, Branch> branchesById = new HashMap<>();

    public EmployeeDirectory() {
        Branch downtown = new Branch("B1", "Downtown", "1 Main St");
        Branch uptown = new Branch("B2", "Uptown", "2 High St");
        branchesById.put(downtown.getBranchId(), downtown);
        branchesById.put(uptown.getBranchId(), uptown);

        addEmployee(new Employee("100000001", "Dana Levi", "050-1000001", "AC-1001", "E-001",
                "dana.l", "secret123", Role.MANAGER, downtown));
        addEmployee(new Employee("100000002", "Yossi Cohen", "050-1000002", "AC-1002", "E-002",
                "yossi.c", "pass456", Role.CASHIER, downtown));
        addEmployee(new Employee("100000003", "Noa Biton", "050-1000003", "AC-1003", "E-003",
                "noa.b", "qwerty789", Role.SELLER, uptown));
    }

    public void addEmployee(Employee employee) {
        employeesByUsername.put(employee.getUsername(), employee);
    }

    public Employee findByUsername(String username) {
        return employeesByUsername.get(username);
    }

    public List<Branch> getBranches() {
        return new ArrayList<>(branchesById.values());
    }

    public Branch findBranchById(String branchId) {
        return branchesById.get(branchId);
    }

    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employeesByUsername.values());
    }
}
