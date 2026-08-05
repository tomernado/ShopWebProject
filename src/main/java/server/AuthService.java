package server;

import logging.SystemLogger;
import model.Employee;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final EmployeeDirectory employeeDirectory;
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    public AuthService(EmployeeDirectory employeeDirectory) {
        this.employeeDirectory = employeeDirectory;
    }

    public LoginResponse login(String username, String password) {
        Employee employee = employeeDirectory.findByUsername(username);

        if (employee == null || !employee.getPassword().equals(password)) {
            return LoginResponse.failure("Invalid username or password");
        }

        // Set.add returns false if the username was already present — one atomic
        // check-and-add, so two threads racing here can't both "win".
        if (!activeSessions.add(username)) {
            return LoginResponse.failure("User already logged in from another location");
        }

        SystemLogger.getInstance().log("EMPLOYEE", "Login: " + username);
        return LoginResponse.success(employee);
    }

    public void logout(String username) {
        if (activeSessions.remove(username)) {
            SystemLogger.getInstance().log("EMPLOYEE", "Logout: " + username);
        }
    }
}
