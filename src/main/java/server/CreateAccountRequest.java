package server;

import model.Role;

import java.io.Serializable;

public class CreateAccountRequest implements Serializable {
    private final String idNumber;
    private final String fullName;
    private final String username;
    private final String password;
    private final Role role;
    private final String branchId;

    public CreateAccountRequest(String idNumber, String fullName, String username, String password, Role role, String branchId) {
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.branchId = branchId;
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

    public String getBranchId() {
        return branchId;
    }
}
