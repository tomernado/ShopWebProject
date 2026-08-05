package server;

public class Main {
    public static void main(String[] args) {
        new Server(5000, new AuthService(new EmployeeDirectory())).start();
    }
}
