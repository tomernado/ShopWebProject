package server;

import logging.SystemLogger;
import model.Branch;
import model.Employee;
import model.Role;

import java.util.List;

public class AccountService {
    private final EmployeeDirectory employeeDirectory;
    private final PasswordPolicy passwordPolicy;

    public AccountService(EmployeeDirectory employeeDirectory, PasswordPolicy passwordPolicy) {
        this.employeeDirectory = employeeDirectory;
        this.passwordPolicy = passwordPolicy;
    }

    public CreateAccountResponse createAccount(Role requesterRole, CreateAccountRequest request) {
        if (requesterRole != Role.MANAGER) {
            return CreateAccountResponse.failure("Only a manager can create accounts");
        }

        String policyError = passwordPolicy.validate(request.getPassword());
        if (policyError != null) {
            return CreateAccountResponse.failure(policyError);
        }

        if (employeeDirectory.findByUsername(request.getUsername()) != null) {
            return CreateAccountResponse.failure("Username already exists");
        }

        Branch branch = employeeDirectory.findBranchById(request.getBranchId());
        if (branch == null) {
            return CreateAccountResponse.failure("Unknown branch");
        }

        Employee newEmployee = new Employee(
                request.getIdNumber(),
                request.getFullName(),
                request.getPhone(),
                request.getAccountNumber(),
                request.getEmployeeNumber(),
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                branch
        );
        employeeDirectory.addEmployee(newEmployee);
        SystemLogger.getInstance().log("ACCOUNT",
                "Created account: " + request.getUsername() + " (" + request.getRole() + ")");

        return CreateAccountResponse.success();
    }

    public List<Employee> getAllEmployees() {
        return employeeDirectory.getAllEmployees();
    }
}
