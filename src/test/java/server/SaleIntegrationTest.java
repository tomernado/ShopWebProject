package server;

import model.CustomerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class SaleIntegrationTest {
    private static final int TEST_PORT = 6300;
    private Server server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void loggedInEmployeeCanRecordASale() throws Exception {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory));
        server = new Server(TEST_PORT, authService, accountService, saleService);

        Thread serverThread = new Thread(server::start);
        serverThread.start();
        Thread.sleep(200);

        try (Socket socket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new LoginRequest("yossi.c", "pass456"));
            out.flush();
            in.readObject(); // LoginResponse

            out.writeObject(new RecordSaleRequest("Customer A", "1", "050", CustomerType.RETURNING, "P1", 2));
            out.flush();

            RecordSaleResponse response = (RecordSaleResponse) in.readObject();

            assertTrue(response.isSuccess());
            assertEquals(12.35, response.getFinalAmount(), 0.0001); // 2 * 6.5 = 13, -5% = 12.35
        }
    }
}
