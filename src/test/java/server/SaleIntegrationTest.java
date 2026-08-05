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
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory), new CustomerDirectory());
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

    @Test
    void inventoryReflectsSaleWhenFetchedAgain() throws Exception {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory), new CustomerDirectory());
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

            out.writeObject(new GetInventoryRequest());
            out.flush();
            GetInventoryResponse before = (GetInventoryResponse) in.readObject();
            int beforeQty = before.getItems().stream()
                    .filter(item -> item.getProductId().equals("P1"))
                    .findFirst().orElseThrow().getAvailableQuantity();

            out.writeObject(new RecordSaleRequest("Customer A", "1", "050", CustomerType.NEW, "P1", 2));
            out.flush();
            in.readObject(); // RecordSaleResponse

            out.writeObject(new GetInventoryRequest());
            out.flush();
            GetInventoryResponse after = (GetInventoryResponse) in.readObject();
            int afterQty = after.getItems().stream()
                    .filter(item -> item.getProductId().equals("P1"))
                    .findFirst().orElseThrow().getAvailableQuantity();

            assertEquals(beforeQty - 2, afterQty);
        }
    }

    @Test
    void customerAppearsInNetworkWideDirectoryAfterASale() throws Exception {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory), new CustomerDirectory());
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

            out.writeObject(new RecordSaleRequest("Dana Customer", "555", "050", CustomerType.NEW, "P1", 1));
            out.flush();
            in.readObject(); // RecordSaleResponse

            out.writeObject(new GetCustomersRequest());
            out.flush();
            GetCustomersResponse customersResponse = (GetCustomersResponse) in.readObject();

            assertEquals(1, customersResponse.getCustomers().size());
            assertEquals("Dana Customer", customersResponse.getCustomers().get(0).getFullName());
        }
    }

    @Test
    void loggedInEmployeeCanListAllNetworkEmployees() throws Exception {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory), new CustomerDirectory());
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

            out.writeObject(new GetEmployeesRequest());
            out.flush();
            GetEmployeesResponse response = (GetEmployeesResponse) in.readObject();

            assertEquals(3, response.getEmployees().size());
        }
    }
}
