package model;

import java.io.Serializable;

public class Employee implements Serializable {
    private final String idNumber;
    private final String fullName;
    private final String phone;
    private final String accountNumber;
    private final String employeeNumber;
    private final String username;
    private final String password;
    private final Role role;
    private final Branch branch;

    public Employee(String idNumber, String fullName, String phone, String accountNumber, String employeeNumber,
                     String username, String password, Role role, Branch branch) {
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.phone = phone;
        this.accountNumber = accountNumber;
        this.employeeNumber = employeeNumber;
        this.username = username;
        this.password = password;
        this.role = role;
        this.branch = branch;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public Branch getBranch() {
        return branch;
    }
}
