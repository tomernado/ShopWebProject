package server;

public class Main {
    public static void main(String[] args) {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        new Server(5000, authService, accountService).start();
    }
}
