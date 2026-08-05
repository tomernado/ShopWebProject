package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ServerIntegrationTest {
    private static final int TEST_PORT = 6000;
    private Server server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void clientCanLogInOverARealSocket() throws Exception {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        server = new Server(TEST_PORT, authService, accountService);
        Thread serverThread = new Thread(server::start);
        serverThread.start();
        Thread.sleep(200); // give the accept loop time to start listening

        try (Socket socket = new Socket("localhost", TEST_PORT);
             // create the output stream before the input stream on both ends —
             // ObjectInputStream's constructor blocks waiting for the other side's
             // stream header, so mismatched order deadlocks the connection.
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new LoginRequest("dana.l", "secret123"));
            out.flush();

            LoginResponse response = (LoginResponse) in.readObject();

            assertTrue(response.isSuccess());
            assertEquals("Dana Levi", response.getEmployee().getFullName());
        }
    }
}
