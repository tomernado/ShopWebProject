package model;

import java.io.Serializable;

public class Employee implements Serializable {
    private final String idNumber;
    private final String fullName;
    private final String username;
    private final String password;
    private final Role role;
    private final Branch branch;

    public Employee(String idNumber, String fullName, String username, String password, Role role, Branch branch) {
        this.idNumber = idNumber;
        this.fullName = fullName;
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
