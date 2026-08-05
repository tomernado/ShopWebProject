package client;

import model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.AccountService;
import server.AuthService;
import server.CreateAccountRequest;
import server.CreateAccountResponse;
import server.EmployeeDirectory;
import server.LoginResponse;
import server.PasswordPolicy;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

class ServerConnectionIntegrationTest {
    private static final int TEST_PORT = 6100;
    private Server server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void managerCreatesAccountAndNewEmployeeCanLogIn() throws Exception {
        startServer();

        try (ServerConnection managerConnection = new ServerConnection("localhost", TEST_PORT)) {
            LoginResponse managerLogin = managerConnection.login("dana.l", "secret123");
            assertTrue(managerLogin.isSuccess());

            CreateAccountResponse createResponse = managerConnection.createAccount(
                    new CreateAccountRequest("100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B1"));
            assertTrue(createResponse.isSuccess());
        }

        try (ServerConnection newEmployeeConnection = new ServerConnection("localhost", TEST_PORT)) {
            LoginResponse newEmployeeLogin = newEmployeeConnection.login("roi.b", "abcdef1");

            assertTrue(newEmployeeLogin.isSuccess());
            assertEquals("Roi Biton", newEmployeeLogin.getEmployee().getFullName());
        }
    }

    @Test
    void nonManagerCannotCreateAnAccount() throws Exception {
        startServer();

        try (ServerConnection cashierConnection = new ServerConnection("localhost", TEST_PORT)) {
            LoginResponse cashierLogin = cashierConnection.login("yossi.c", "pass456");
            assertTrue(cashierLogin.isSuccess());

            CreateAccountResponse response = cashierConnection.createAccount(
                    new CreateAccountRequest("100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B1"));

            assertFalse(response.isSuccess());
        }
    }

    private void startServer() throws InterruptedException {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        server = new Server(TEST_PORT, authService, accountService);

        Thread serverThread = new Thread(server::start);
        serverThread.start();
        Thread.sleep(200);
    }
}
