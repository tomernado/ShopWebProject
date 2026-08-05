# Stage 5 — Reports & I/O (Logs, JSON, Word) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Comprehensive logging (employees, accounts, sales, chat), a minimal real sale operation that finally exercises Stage 1's `Customer` polymorphism and `Inventory` in production flow, and statistical reports (by branch/product/category) exportable to real JSON and real `.docx` Word documents.

**Architecture:** `logging.SystemLogger` is a second Singleton (same shape as Stage 4's `ChatDispatcher`) — every existing service logs through `getInstance()`, no constructor changes needed for it anywhere. `server.SaleService` follows the exact same shape as `AuthService`/`AccountService`. `reports.ReportGenerator` is pure aggregation logic; `JsonReportExporter`/`WordReportExporter` are thin, independently-testable file writers.

**Tech Stack:** Java 17, Maven, JUnit 5, plus two new production dependencies: Gson (JSON) and Apache POI `poi-ooxml` (real `.docx` generation — confirmed as an official graded requirement, not a shortcut-able extra).

## Global Constraints

- Academic course project — every line explainable by the student.
- Builds on Stages 1-4, all merged to `master`. Design: [2026-08-05-stage5-reports-design.md](../specs/2026-08-05-stage5-reports-design.md).
- `SystemLogger` only logs successful events (login, logout, account creation, sale, chat) — failures are not logged, a documented simplification.
- Chat message content in logs is gated by `ChatDispatcher.LOG_CHAT_CONTENT` (`false` by default) — the "option to save chat content" from the original requirement.
- Reports are **not** wired into the network protocol — `SaleService`/`RecordSaleRequest` **are** (the purchase action itself is a real client-server operation), but report generation/export stays a server-side service proven by tests and a runnable `reports.Main` demo, matching how Stage 4 kept chat GUI out of scope.
- `logs/` and `reports/` (generated output directories) are gitignored — they're runtime artifacts, not source.
- Out of scope: any GUI for purchases or reports, persisting the sales ledger to disk/DB (still in-memory, same status as `EmployeeDirectory` since Stage 2).

---

### Task 1: `logging.SystemLogger`

**Files:**
- Create: `src/main/java/logging/SystemLogger.java`
- Test: `src/test/java/logging/SystemLoggerTest.java`
- Modify: `.gitignore`

**Interfaces:**
- Produces: `SystemLogger.getInstance()` (production singleton), `SystemLogger(Path)` (public constructor, test-only use), `log(String category, String message)`. Consumed by Task 2 and `SaleService` (Task 4).

- [ ] **Step 1: Add `logs/` and `reports/` to `.gitignore`**

Add these two lines to `.gitignore`:

```
logs/
reports/
```

- [ ] **Step 2: Write the failing test**

```java
package logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemLoggerTest {

    @Test
    void logAppendsAFormattedLineToTheFile(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("test.log");
        SystemLogger logger = new SystemLogger(logFile);

        logger.log("EMPLOYEE", "dana.l logged in");

        String content = Files.readString(logFile);
        assertTrue(content.contains("[EMPLOYEE]"));
        assertTrue(content.contains("dana.l logged in"));
    }

    @Test
    void multipleLogCallsAppendRatherThanOverwrite(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("test.log");
        SystemLogger logger = new SystemLogger(logFile);

        logger.log("EMPLOYEE", "first entry");
        logger.log("EMPLOYEE", "second entry");

        String content = Files.readString(logFile);
        assertTrue(content.contains("first entry"));
        assertTrue(content.contains("second entry"));
    }
}
```

Save as `src/test/java/logging/SystemLoggerTest.java`.

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn test -Dtest=SystemLoggerTest`
Expected: build failure — `cannot find symbol: class SystemLogger`.

- [ ] **Step 4: Implement `SystemLogger`**

```java
package logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemLogger {
    private static final SystemLogger INSTANCE = new SystemLogger(Path.of("logs", "system.log"));
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFilePath;

    // Public in addition to the Singleton accessor — exists only for tests, so
    // each test can log to its own isolated temp file instead of the shared
    // production log. Same accommodation as ChatDispatcher.resetForTests().
    public SystemLogger(Path logFilePath) {
        this.logFilePath = logFilePath;
        createParentDirectoryIfNeeded();
    }

    public static SystemLogger getInstance() {
        return INSTANCE;
    }

    public synchronized void log(String category, String message) {
        String line = "[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] [" + category + "] " + message;
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath.toFile(), true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Failed to write log entry: " + e.getMessage());
        }
    }

    private void createParentDirectoryIfNeeded() {
        try {
            if (logFilePath.getParent() != null) {
                Files.createDirectories(logFilePath.getParent());
            }
        } catch (IOException e) {
            System.err.println("Could not create log directory: " + e.getMessage());
        }
    }
}
```

Save as `src/main/java/logging/SystemLogger.java`.

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn test -Dtest=SystemLoggerTest`
Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/logging/SystemLogger.java src/test/java/logging/SystemLoggerTest.java .gitignore
git commit -m "feat: add SystemLogger (Singleton) for append-only system logs"
```

---

### Task 2: Wire logging into `AuthService`, `AccountService`, `ChatDispatcher`

**Files:**
- Modify: `src/main/java/server/AuthService.java`
- Modify: `src/main/java/server/AccountService.java`
- Modify: `src/main/java/chat/ChatDispatcher.java`

**Interfaces:**
- No public signature changes — only internal log calls added. No new failing test; verified by the existing suite still passing (Step 3).

- [ ] **Step 1: Add logging to `AuthService`**

In `src/main/java/server/AuthService.java`, add the import:

```java
import logging.SystemLogger;
```

Replace the `login` and `logout` methods with:

```java
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
```

- [ ] **Step 2: Add logging to `AccountService` and `ChatDispatcher`**

In `src/main/java/server/AccountService.java`, add the import `import logging.SystemLogger;` and change the end of `createAccount`:

```java
        employeeDirectory.addEmployee(newEmployee);
        SystemLogger.getInstance().log("ACCOUNT",
                "Created account: " + request.getUsername() + " (" + request.getRole() + ")");

        return CreateAccountResponse.success();
```

In `src/main/java/chat/ChatDispatcher.java`, add the import `import logging.SystemLogger;` and this field:

```java
    private static final boolean LOG_CHAT_CONTENT = false;
```

Update `requestChat` to log right after creating the session (before the two `send`/`return` lines):

```java
        ChatSession session = new ChatSession(requester, partner);
        sessionsById.put(session.getSessionId(), session);

        SystemLogger.getInstance().log("CHAT", "Chat started: session=" + session.getSessionId()
                + " between " + requester.getEmployee().getUsername() + " and " + partner.getEmployee().getUsername());

        partner.send(new ChatStarted(session.getSessionId(), requester.getEmployee()));
        return new ChatStarted(session.getSessionId(), partner.getEmployee());
```

Update `joinChat` to log right after `session.addParticipant(participant)`:

```java
        session.addParticipant(participant);
        SystemLogger.getInstance().log("CHAT", "Manager " + participant.getEmployee().getUsername()
                + " joined session " + sessionId);
        session.broadcast(new ParticipantJoined(sessionId, participant.getEmployee()), participant);
        return JoinChatResponse.success();
```

Update `sendMessage`:

```java
    public synchronized void sendMessage(String sessionId, ChatParticipant sender, String text) {
        ChatSession session = sessionsById.get(sessionId);
        if (session == null) {
            return;
        }

        String detail = LOG_CHAT_CONTENT
                ? "Message in " + sessionId + " from " + sender.getEmployee().getUsername() + ": " + text
                : "Message sent in session " + sessionId + " by " + sender.getEmployee().getUsername();
        SystemLogger.getInstance().log("CHAT", detail);

        session.broadcast(new ChatMessage(sessionId, sender.getEmployee(), text), sender);
    }
```

- [ ] **Step 3: Run the full existing suite to confirm nothing broke**

Run: `mvn test`
Expected: `BUILD SUCCESS`. All 49 existing tests (47 from Stages 1-4 + the 2 new `SystemLoggerTest` tests) pass. A `logs/system.log` file will appear in the project root as a side effect — this is expected and gitignored.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/server/AuthService.java src/main/java/server/AccountService.java src/main/java/chat/ChatDispatcher.java
git commit -m "feat: log employee, account, and chat events through SystemLogger"
```

---

### Task 3: `server.ProductCatalog`

**Files:**
- Create: `src/main/java/server/ProductCatalog.java`
- Test: `src/test/java/server/ProductCatalogTest.java`

**Interfaces:**
- Consumes: `EmployeeDirectory.getBranches()` (Stage 3).
- Produces: `ProductCatalog(EmployeeDirectory)`, `findProductById(String)` returning `Product` or `null`, `getInventoryForBranch(String branchId)` returning `Inventory` or `null`. Consumed by `SaleService` (Task 4).

- [ ] **Step 1: Write the failing test**

```java
package server;

import model.Product;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogTest {
    private final ProductCatalog catalog = new ProductCatalog(new EmployeeDirectory());

    @Test
    void findsSeededProductById() {
        Product product = catalog.findProductById("P1");

        assertNotNull(product);
        assertEquals("Milk", product.getName());
    }

    @Test
    void unknownProductIdReturnsNull() {
        assertNull(catalog.findProductById("nope"));
    }

    @Test
    void eachSeededBranchHasStockedInventory() {
        var inventory = catalog.getInventoryForBranch("B1");
        Product milk = catalog.findProductById("P1");

        assertNotNull(inventory);
        assertEquals(50, inventory.getQuantity(milk));
    }

    @Test
    void unknownBranchHasNoInventory() {
        assertNull(catalog.getInventoryForBranch("nope"));
    }
}
```

Save as `src/test/java/server/ProductCatalogTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=ProductCatalogTest`
Expected: build failure — `cannot find symbol: class ProductCatalog`.

- [ ] **Step 3: Implement `ProductCatalog`**

```java
package server;

import model.Branch;
import model.Inventory;
import model.Product;

import java.util.HashMap;
import java.util.Map;

public class ProductCatalog {
    private final Map<String, Product> productsById = new HashMap<>();
    private final Map<String, Inventory> inventoryByBranchId = new HashMap<>();

    public ProductCatalog(EmployeeDirectory employeeDirectory) {
        Product milk = new Product("P1", "Milk", "Dairy", 6.5);
        Product bread = new Product("P2", "Bread", "Bakery", 8.0);
        Product cheese = new Product("P3", "Cheese", "Dairy", 22.0);

        productsById.put(milk.getProductId(), milk);
        productsById.put(bread.getProductId(), bread);
        productsById.put(cheese.getProductId(), cheese);

        for (Branch branch : employeeDirectory.getBranches()) {
            Inventory inventory = new Inventory(branch);
            inventory.addStock(milk, 50);
            inventory.addStock(bread, 30);
            inventory.addStock(cheese, 20);
            inventoryByBranchId.put(branch.getBranchId(), inventory);
        }
    }

    public Product findProductById(String productId) {
        return productsById.get(productId);
    }

    public Inventory getInventoryForBranch(String branchId) {
        return inventoryByBranchId.get(branchId);
    }
}
```

Save as `src/main/java/server/ProductCatalog.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=ProductCatalogTest`
Expected: `BUILD SUCCESS`, 4 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/ProductCatalog.java src/test/java/server/ProductCatalogTest.java
git commit -m "feat: add ProductCatalog with seeded products and per-branch inventory"
```

---

### Task 4: `SaleRecord`, `RecordSaleRequest`/`RecordSaleResponse`, `SaleService`

**Files:**
- Create: `src/main/java/server/SaleRecord.java`
- Create: `src/main/java/server/RecordSaleRequest.java`
- Create: `src/main/java/server/RecordSaleResponse.java`
- Create: `src/main/java/server/SaleService.java`
- Test: `src/test/java/server/SaleServiceTest.java`

**Interfaces:**
- Consumes: `ProductCatalog` (Task 3), `Customer`/`NewCustomer`/`ReturningCustomer`/`VipCustomer`/`Inventory` (Stage 1), `SystemLogger` (Task 1).
- Produces: `SaleService(ProductCatalog)`, `recordSale(Employee, RecordSaleRequest)` returning `RecordSaleResponse`, `getSalesLedger()` returning `List<SaleRecord>`. Consumed by `ClientHandler` (Task 5).

- [ ] **Step 1: Write the failing test**

```java
package server;

import model.Branch;
import model.CustomerType;
import model.Employee;
import model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaleServiceTest {
    private final EmployeeDirectory employeeDirectory = new EmployeeDirectory();
    private final ProductCatalog productCatalog = new ProductCatalog(employeeDirectory);
    private final SaleService saleService = new SaleService(productCatalog);
    private final Branch branch = employeeDirectory.findBranchById("B1");
    private final Employee cashier = new Employee("1", "Yossi Cohen", "yossi.c", "pw", Role.CASHIER, branch);

    @Test
    void newCustomerPaysFullPriceAndStockIsReduced() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "P1", 2);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertTrue(response.isSuccess());
        assertEquals(13.0, response.getFinalAmount(), 0.0001); // 2 * 6.5, no discount
        assertEquals(48, productCatalog.getInventoryForBranch("B1").getQuantity(productCatalog.findProductById("P1")));
    }

    @Test
    void vipCustomerGetsTenPercentDiscount() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer B", "2", "050", CustomerType.VIP, "P1", 2);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertEquals(11.7, response.getFinalAmount(), 0.0001); // 13 - 10%
    }

    @Test
    void unknownProductFails() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "no-such-product", 1);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void insufficientStockFails() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "P1", 1000);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void successfulSaleIsAddedToTheLedger() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "P1", 1);

        saleService.recordSale(cashier, request);

        assertEquals(1, saleService.getSalesLedger().size());
        assertEquals("Customer A", saleService.getSalesLedger().get(0).getCustomerFullName());
    }
}
```

Save as `src/test/java/server/SaleServiceTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=SaleServiceTest`
Expected: build failure — none of `RecordSaleRequest`, `RecordSaleResponse`, `SaleService` exist yet.

- [ ] **Step 3: Implement `SaleRecord`, `RecordSaleRequest`, `RecordSaleResponse`, `SaleService`**

```java
package server;

import model.CustomerType;

import java.time.LocalDateTime;

public class SaleRecord {
    private final String branchId;
    private final String employeeUsername;
    private final String customerFullName;
    private final CustomerType customerType;
    private final String productId;
    private final String productName;
    private final String category;
    private final int quantity;
    private final double finalAmount;
    private final LocalDateTime timestamp;

    public SaleRecord(String branchId, String employeeUsername, String customerFullName, CustomerType customerType,
                       String productId, String productName, String category, int quantity, double finalAmount,
                       LocalDateTime timestamp) {
        this.branchId = branchId;
        this.employeeUsername = employeeUsername;
        this.customerFullName = customerFullName;
        this.customerType = customerType;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.quantity = quantity;
        this.finalAmount = finalAmount;
        this.timestamp = timestamp;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getEmployeeUsername() {
        return employeeUsername;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
```

Save as `src/main/java/server/SaleRecord.java`.

```java
package server;

import model.CustomerType;

import java.io.Serializable;

public class RecordSaleRequest implements Serializable {
    private final String customerFullName;
    private final String customerIdNumber;
    private final String customerPhone;
    private final CustomerType customerType;
    private final String productId;
    private final int quantity;

    public RecordSaleRequest(String customerFullName, String customerIdNumber, String customerPhone,
                              CustomerType customerType, String productId, int quantity) {
        this.customerFullName = customerFullName;
        this.customerIdNumber = customerIdNumber;
        this.customerPhone = customerPhone;
        this.customerType = customerType;
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public String getCustomerIdNumber() {
        return customerIdNumber;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
```

Save as `src/main/java/server/RecordSaleRequest.java`.

```java
package server;

import java.io.Serializable;

public class RecordSaleResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;
    private final double finalAmount;

    private RecordSaleResponse(boolean success, String errorMessage, double finalAmount) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.finalAmount = finalAmount;
    }

    public static RecordSaleResponse success(double finalAmount) {
        return new RecordSaleResponse(true, null, finalAmount);
    }

    public static RecordSaleResponse failure(String errorMessage) {
        return new RecordSaleResponse(false, errorMessage, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinalAmount() {
        return finalAmount;
    }
}
```

Save as `src/main/java/server/RecordSaleResponse.java`.

```java
package server;

import logging.SystemLogger;
import model.Customer;
import model.Employee;
import model.Inventory;
import model.NewCustomer;
import model.Product;
import model.ReturningCustomer;
import model.VipCustomer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaleService {
    private final ProductCatalog productCatalog;
    private final List<SaleRecord> salesLedger = new ArrayList<>();

    public SaleService(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    public synchronized RecordSaleResponse recordSale(Employee employee, RecordSaleRequest request) {
        if (request.getQuantity() <= 0) {
            return RecordSaleResponse.failure("Quantity must be positive");
        }

        Product product = productCatalog.findProductById(request.getProductId());
        if (product == null) {
            return RecordSaleResponse.failure("Unknown product");
        }

        Inventory inventory = productCatalog.getInventoryForBranch(employee.getBranch().getBranchId());
        if (inventory.getQuantity(product) < request.getQuantity()) {
            return RecordSaleResponse.failure("Not enough stock");
        }

        Customer customer = buildCustomer(request);
        double totalBeforeDiscount = product.getPrice() * request.getQuantity();
        double finalAmount = customer.purchase(totalBeforeDiscount);

        inventory.reduceStock(product, request.getQuantity());

        SaleRecord record = new SaleRecord(
                employee.getBranch().getBranchId(),
                employee.getUsername(),
                request.getCustomerFullName(),
                request.getCustomerType(),
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                request.getQuantity(),
                finalAmount,
                LocalDateTime.now()
        );
        salesLedger.add(record);

        SystemLogger.getInstance().log("SALE", String.format(
                "%s sold %d x %s at branch %s for %.2f (customer: %s, %s)",
                employee.getUsername(), request.getQuantity(), product.getName(),
                employee.getBranch().getBranchId(), finalAmount,
                request.getCustomerFullName(), request.getCustomerType()));

        return RecordSaleResponse.success(finalAmount);
    }

    private Customer buildCustomer(RecordSaleRequest request) {
        String name = request.getCustomerFullName();
        String id = request.getCustomerIdNumber();
        String phone = request.getCustomerPhone();
        return switch (request.getCustomerType()) {
            case NEW -> new NewCustomer(name, id, phone);
            case RETURNING -> new ReturningCustomer(name, id, phone);
            case VIP -> new VipCustomer(name, id, phone);
        };
    }

    public synchronized List<SaleRecord> getSalesLedger() {
        return Collections.unmodifiableList(new ArrayList<>(salesLedger));
    }
}
```

Save as `src/main/java/server/SaleService.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=SaleServiceTest`
Expected: `BUILD SUCCESS`, 5 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/SaleRecord.java src/main/java/server/RecordSaleRequest.java src/main/java/server/RecordSaleResponse.java src/main/java/server/SaleService.java src/test/java/server/SaleServiceTest.java
git commit -m "feat: add SaleService — Stage 1's Customer polymorphism now runs in production flow"
```

---

### Task 5: Wire `SaleService` into `ClientHandler`/`Server`/`Main` + `SaleIntegrationTest`

**Files:**
- Modify: `src/main/java/server/ClientHandler.java`
- Modify: `src/main/java/server/Server.java`
- Modify: `src/main/java/server/Main.java`
- Modify: `src/test/java/server/ServerIntegrationTest.java`
- Modify: `src/test/java/client/ServerConnectionIntegrationTest.java`
- Modify: `src/test/java/server/ChatIntegrationTest.java`
- Test: `src/test/java/server/SaleIntegrationTest.java`

**Interfaces:**
- Produces: `Server(int port, AuthService, AccountService, SaleService)` (constructor signature changes — one new parameter). `ClientHandler(Socket, AuthService, AccountService, SaleService)`.

- [ ] **Step 1: Update the three existing integration tests' server-setup blocks (failing until Step 3)**

In `src/test/java/server/ServerIntegrationTest.java`, `src/test/java/client/ServerConnectionIntegrationTest.java`, and `src/test/java/server/ChatIntegrationTest.java`, each has a block that looks like:

```java
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        server = new Server(TEST_PORT, authService, accountService);
```

Replace it (in all three files) with:

```java
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory));
        server = new Server(TEST_PORT, authService, accountService, saleService);
```

(`ServerConnectionIntegrationTest.java` is in the `client` package, so it also needs `import server.SaleService;` and `import server.ProductCatalog;` added alongside its existing `server.*` imports.)

- [ ] **Step 2: Run the existing suite to verify it fails**

Run: `mvn test`
Expected: build failure — no constructor `Server(int, AuthService, AccountService, SaleService)` yet.

- [ ] **Step 3: Update `ClientHandler`, `Server`, `Main`**

```java
package server;

import chat.ActiveChatsResponse;
import chat.ChatDispatcher;
import chat.ChatMessage;
import chat.ChatParticipant;
import chat.ChatRequest;
import chat.JoinChatRequest;
import chat.ListActiveChatsRequest;
import model.Employee;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable, ChatParticipant {
    private final Socket socket;
    private final AuthService authService;
    private final AccountService accountService;
    private final SaleService saleService;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Employee loggedInEmployee;

    public ClientHandler(Socket socket, AuthService authService, AccountService accountService, SaleService saleService) {
        this.socket = socket;
        this.authService = authService;
        this.accountService = accountService;
        this.saleService = saleService;
    }

    @Override
    public Employee getEmployee() {
        return loggedInEmployee;
    }

    @Override
    public synchronized void send(Object message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            String username = loggedInEmployee != null ? loggedInEmployee.getUsername() : "unknown";
            System.err.println("Failed to push message to " + username + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            LoginRequest request = (LoginRequest) in.readObject();
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            send(response);

            if (response.isSuccess()) {
                loggedInEmployee = response.getEmployee();
                handleAuthenticatedSession();
            }
        } catch (EOFException e) {
            // client disconnected — normal end of the read loop
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            if (loggedInEmployee != null) {
                authService.logout(loggedInEmployee.getUsername());
            }
            closeSocket();
        }
    }

    private void handleAuthenticatedSession() throws IOException, ClassNotFoundException {
        while (true) {
            Object message = in.readObject();
            if (message instanceof CreateAccountRequest request) {
                send(accountService.createAccount(loggedInEmployee.getRole(), request));
            } else if (message instanceof RecordSaleRequest request) {
                send(saleService.recordSale(loggedInEmployee, request));
            } else if (message instanceof ChatRequest) {
                send(ChatDispatcher.getInstance().requestChat(this));
            } else if (message instanceof JoinChatRequest request) {
                send(ChatDispatcher.getInstance().joinChat(request.getSessionId(), this));
            } else if (message instanceof ChatMessage chatMessage) {
                ChatDispatcher.getInstance().sendMessage(chatMessage.getSessionId(), this, chatMessage.getText());
            } else if (message instanceof ListActiveChatsRequest) {
                send(new ActiveChatsResponse(ChatDispatcher.getInstance().listActiveChats()));
            }
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // socket already closed
        }
    }
}
```

Save as `src/main/java/server/ClientHandler.java` (replaces the Stage 4 version).

```java
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;
    private final AuthService authService;
    private final AccountService accountService;
    private final SaleService saleService;
    private ServerSocket serverSocket;

    public Server(int port, AuthService authService, AccountService accountService, SaleService saleService) {
        this.port = port;
        this.authService = authService;
        this.accountService = accountService;
        this.saleService = saleService;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, authService, accountService, saleService)).start();
            }
        } catch (IOException e) {
            // Expected once stop() closes the server socket while accept() is blocked.
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // socket already closed
        }
    }
}
```

Save as `src/main/java/server/Server.java` (replaces the Stage 2 version).

```java
package server;

public class Main {
    public static void main(String[] args) {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory));
        new Server(5000, authService, accountService, saleService).start();
    }
}
```

Save as `src/main/java/server/Main.java` (replaces the Stage 3 version).

- [ ] **Step 4: Run the full suite to verify the three updated tests pass again**

Run: `mvn test`
Expected: `BUILD SUCCESS`. All 51 existing tests pass (49 from Stages 1-4 + Task 1's 2 `SystemLoggerTest` + Task 3's 4 `ProductCatalogTest` + Task 4's 5 `SaleServiceTest` — the running total by now is 49 + 2 + 4 + 5 = 60; if your count differs, trust `mvn test`'s own summary line over this note).

- [ ] **Step 5: Write the failing `SaleIntegrationTest`**

```java
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
```

Save as `src/test/java/server/SaleIntegrationTest.java`.

- [ ] **Step 6: Run the test to verify it passes**

Run: `mvn test -Dtest=SaleIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 7: Run the full test suite**

Run: `mvn test`
Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/server/ClientHandler.java src/main/java/server/Server.java src/main/java/server/Main.java src/test/java/server/ServerIntegrationTest.java src/test/java/client/ServerConnectionIntegrationTest.java src/test/java/server/ChatIntegrationTest.java src/test/java/server/SaleIntegrationTest.java
git commit -m "feat: dispatch RecordSaleRequest on authenticated connections"
```

---

### Task 6: `reports.SalesReportLine`/`SalesReport`/`ReportGenerator`

**Files:**
- Create: `src/main/java/reports/SalesReportLine.java`
- Create: `src/main/java/reports/SalesReport.java`
- Create: `src/main/java/reports/ReportGenerator.java`
- Test: `src/test/java/reports/ReportGeneratorTest.java`

**Interfaces:**
- Consumes: `server.SaleRecord` (Task 4).
- Produces: `SalesReport.getGroupedBy()`/`getLines()`, `SalesReportLine.getKey()`/`getTotalQuantity()`/`getTotalRevenue()`, `ReportGenerator.byBranch/byProduct/byCategory(List<SaleRecord>)` returning `SalesReport`. Consumed by `JsonReportExporter`/`WordReportExporter` (Tasks 7-8) and `reports.Main` (Task 9).

- [ ] **Step 1: Write the failing test**

```java
package reports;

import model.CustomerType;
import org.junit.jupiter.api.Test;
import server.SaleRecord;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {
    private final ReportGenerator generator = new ReportGenerator();

    private SaleRecord sale(String branchId, String productName, String category, int quantity, double amount) {
        return new SaleRecord(branchId, "yossi.c", "Customer A", CustomerType.NEW,
                "P1", productName, category, quantity, amount, LocalDateTime.now());
    }

    @Test
    void byBranchAggregatesQuantityAndRevenuePerBranch() {
        List<SaleRecord> sales = List.of(
                sale("B1", "Milk", "Dairy", 2, 13.0),
                sale("B1", "Bread", "Bakery", 1, 8.0),
                sale("B2", "Milk", "Dairy", 3, 19.5)
        );

        SalesReport report = generator.byBranch(sales);

        assertEquals("branch", report.getGroupedBy());
        assertEquals(2, report.getLines().size());

        SalesReportLine b1 = findLine(report, "B1");
        assertEquals(3, b1.getTotalQuantity());
        assertEquals(21.0, b1.getTotalRevenue(), 0.0001);
    }

    @Test
    void byProductAggregatesAcrossBranches() {
        List<SaleRecord> sales = List.of(
                sale("B1", "Milk", "Dairy", 2, 13.0),
                sale("B2", "Milk", "Dairy", 3, 19.5)
        );

        SalesReport report = generator.byProduct(sales);

        SalesReportLine milk = findLine(report, "Milk");
        assertEquals(5, milk.getTotalQuantity());
        assertEquals(32.5, milk.getTotalRevenue(), 0.0001);
    }

    @Test
    void byCategoryGroupsDifferentProductsTogether() {
        List<SaleRecord> sales = List.of(
                sale("B1", "Milk", "Dairy", 2, 13.0),
                sale("B1", "Cheese", "Dairy", 1, 22.0)
        );

        SalesReport report = generator.byCategory(sales);

        assertEquals(1, report.getLines().size());
        SalesReportLine dairy = findLine(report, "Dairy");
        assertEquals(3, dairy.getTotalQuantity());
        assertEquals(35.0, dairy.getTotalRevenue(), 0.0001);
    }

    @Test
    void emptySalesListProducesAnEmptyReport() {
        SalesReport report = generator.byBranch(List.of());

        assertTrue(report.getLines().isEmpty());
    }

    private SalesReportLine findLine(SalesReport report, String key) {
        return report.getLines().stream()
                .filter(line -> line.getKey().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
```

Save as `src/test/java/reports/ReportGeneratorTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=ReportGeneratorTest`
Expected: build failure — none of `ReportGenerator`, `SalesReport`, `SalesReportLine` exist yet.

- [ ] **Step 3: Implement `SalesReportLine`, `SalesReport`, `ReportGenerator`**

```java
package reports;

import java.io.Serializable;

public class SalesReportLine implements Serializable {
    private final String key;
    private final int totalQuantity;
    private final double totalRevenue;

    public SalesReportLine(String key, int totalQuantity, double totalRevenue) {
        this.key = key;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }

    public String getKey() {
        return key;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }
}
```

Save as `src/main/java/reports/SalesReportLine.java`.

```java
package reports;

import java.io.Serializable;
import java.util.List;

public class SalesReport implements Serializable {
    private final String groupedBy;
    private final List<SalesReportLine> lines;

    public SalesReport(String groupedBy, List<SalesReportLine> lines) {
        this.groupedBy = groupedBy;
        this.lines = lines;
    }

    public String getGroupedBy() {
        return groupedBy;
    }

    public List<SalesReportLine> getLines() {
        return lines;
    }
}
```

Save as `src/main/java/reports/SalesReport.java`.

```java
package reports;

import server.SaleRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReportGenerator {

    public SalesReport byBranch(List<SaleRecord> sales) {
        return group("branch", sales, SaleRecord::getBranchId);
    }

    public SalesReport byProduct(List<SaleRecord> sales) {
        return group("product", sales, SaleRecord::getProductName);
    }

    public SalesReport byCategory(List<SaleRecord> sales) {
        return group("category", sales, SaleRecord::getCategory);
    }

    private SalesReport group(String groupedBy, List<SaleRecord> sales, Function<SaleRecord, String> keyExtractor) {
        Map<String, Integer> quantityByKey = new LinkedHashMap<>();
        Map<String, Double> revenueByKey = new LinkedHashMap<>();

        for (SaleRecord sale : sales) {
            String key = keyExtractor.apply(sale);
            quantityByKey.merge(key, sale.getQuantity(), Integer::sum);
            revenueByKey.merge(key, sale.getFinalAmount(), Double::sum);
        }

        List<SalesReportLine> lines = new ArrayList<>();
        for (String key : quantityByKey.keySet()) {
            lines.add(new SalesReportLine(key, quantityByKey.get(key), revenueByKey.get(key)));
        }

        return new SalesReport(groupedBy, lines);
    }
}
```

Save as `src/main/java/reports/ReportGenerator.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=ReportGeneratorTest`
Expected: `BUILD SUCCESS`, 4 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/reports/SalesReportLine.java src/main/java/reports/SalesReport.java src/main/java/reports/ReportGenerator.java src/test/java/reports/ReportGeneratorTest.java
git commit -m "feat: add ReportGenerator with branch/product/category aggregation"
```

---

### Task 7: Gson dependency + `reports.JsonReportExporter`

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/reports/JsonReportExporter.java`
- Test: `src/test/java/reports/JsonReportExporterTest.java`

**Interfaces:**
- Produces: `JsonReportExporter.export(SalesReport, Path)` (throws `IOException`). Consumed by `reports.Main` (Task 9).

- [ ] **Step 1: Add the Gson dependency**

In `pom.xml`, inside `<dependencies>`, add (alongside the existing `junit-jupiter` dependency):

```xml
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>
```

- [ ] **Step 2: Write the failing test**

```java
package reports;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonReportExporterTest {
    private final JsonReportExporter exporter = new JsonReportExporter();

    @Test
    void exportedFileParsesBackToTheSameReport(@TempDir Path tempDir) throws Exception {
        SalesReport report = new SalesReport("branch", List.of(
                new SalesReportLine("B1", 3, 21.0),
                new SalesReportLine("B2", 3, 19.5)
        ));
        Path targetFile = tempDir.resolve("report.json");

        exporter.export(report, targetFile);

        assertTrue(Files.exists(targetFile));
        SalesReport parsedBack = new Gson().fromJson(Files.readString(targetFile), SalesReport.class);
        assertEquals("branch", parsedBack.getGroupedBy());
        assertEquals(2, parsedBack.getLines().size());
        assertEquals("B1", parsedBack.getLines().get(0).getKey());
        assertEquals(21.0, parsedBack.getLines().get(0).getTotalRevenue(), 0.0001);
    }
}
```

Save as `src/test/java/reports/JsonReportExporterTest.java`.

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn test -Dtest=JsonReportExporterTest`
Expected: build failure — `cannot find symbol: class JsonReportExporter`.

- [ ] **Step 4: Implement `JsonReportExporter`**

```java
package reports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonReportExporter {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void export(SalesReport report, Path targetFile) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        Files.writeString(targetFile, gson.toJson(report));
    }
}
```

Save as `src/main/java/reports/JsonReportExporter.java`.

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn test -Dtest=JsonReportExporterTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/reports/JsonReportExporter.java src/test/java/reports/JsonReportExporterTest.java
git commit -m "feat: add JSON report export via Gson"
```

---

### Task 8: Apache POI dependency + `reports.WordReportExporter`

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/reports/WordReportExporter.java`
- Test: `src/test/java/reports/WordReportExporterTest.java`

**Interfaces:**
- Produces: `WordReportExporter.export(SalesReport, Path)` (throws `IOException`). Consumed by `reports.Main` (Task 9).

- [ ] **Step 1: Add the Apache POI dependency**

In `pom.xml`, inside `<dependencies>`, add:

```xml
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
```

- [ ] **Step 2: Write the failing test**

```java
package reports;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordReportExporterTest {
    private final WordReportExporter exporter = new WordReportExporter();

    @Test
    void exportedDocxContainsTheTitleAndTableData(@TempDir Path tempDir) throws Exception {
        SalesReport report = new SalesReport("product", List.of(
                new SalesReportLine("Milk", 5, 32.5)
        ));
        Path targetFile = tempDir.resolve("report.docx");

        exporter.export(report, targetFile);

        assertTrue(Files.exists(targetFile));

        try (FileInputStream in = new FileInputStream(targetFile.toFile());
             XWPFDocument document = new XWPFDocument(in)) {

            String titleText = document.getParagraphs().get(0).getText();
            assertTrue(titleText.contains("product"));

            XWPFTable table = document.getTables().get(0);
            assertEquals("product", table.getRow(0).getCell(0).getText());
            assertEquals("Milk", table.getRow(1).getCell(0).getText());
            assertEquals("5", table.getRow(1).getCell(1).getText());
            assertEquals("32.50", table.getRow(1).getCell(2).getText());
        }
    }
}
```

Save as `src/test/java/reports/WordReportExporterTest.java`.

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn test -Dtest=WordReportExporterTest`
Expected: build failure — `cannot find symbol: class WordReportExporter`.

- [ ] **Step 4: Implement `WordReportExporter`**

```java
package reports;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WordReportExporter {

    public void export(SalesReport report, Path targetFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            writeTitle(document, report);
            writeTable(document, report);
            writeToFile(document, targetFile);
        }
    }

    private void writeTitle(XWPFDocument document, SalesReport report) {
        XWPFParagraph title = document.createParagraph();
        XWPFRun titleRun = title.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setText("Sales Report - grouped by " + report.getGroupedBy());
    }

    private void writeTable(XWPFDocument document, SalesReport report) {
        XWPFTable table = document.createTable(report.getLines().size() + 1, 3);

        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText(report.getGroupedBy());
        header.getCell(1).setText("Total Quantity");
        header.getCell(2).setText("Total Revenue");

        for (int i = 0; i < report.getLines().size(); i++) {
            SalesReportLine line = report.getLines().get(i);
            XWPFTableRow row = table.getRow(i + 1);
            row.getCell(0).setText(line.getKey());
            row.getCell(1).setText(String.valueOf(line.getTotalQuantity()));
            row.getCell(2).setText(String.format("%.2f", line.getTotalRevenue()));
        }
    }

    private void writeToFile(XWPFDocument document, Path targetFile) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        try (FileOutputStream out = new FileOutputStream(targetFile.toFile())) {
            document.write(out);
        }
    }
}
```

Save as `src/main/java/reports/WordReportExporter.java`.

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn test -Dtest=WordReportExporterTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures. This test doesn't just check the file exists — it reads the `.docx` back with POI and asserts the actual title and table cell contents, proving the file is a real, readable Word document.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/reports/WordReportExporter.java src/test/java/reports/WordReportExporterTest.java
git commit -m "feat: add real .docx report export via Apache POI"
```

---

### Task 9: `reports.Main` — runnable demo producing real files

**Files:**
- Create: `src/main/java/reports/Main.java`

**Interfaces:**
- No new interfaces — this is the last task in the plan. Not covered by JUnit (an entry point, same status as `server.Main`/`client.Main`); verified manually in this task's Step 2.

- [ ] **Step 1: Implement `reports.Main`**

```java
package reports;

import model.CustomerType;
import model.Employee;
import server.EmployeeDirectory;
import server.ProductCatalog;
import server.RecordSaleRequest;
import server.SaleService;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        ProductCatalog productCatalog = new ProductCatalog(employeeDirectory);
        SaleService saleService = new SaleService(productCatalog);

        Employee dana = employeeDirectory.findByUsername("dana.l");
        Employee yossi = employeeDirectory.findByUsername("yossi.c");
        Employee noa = employeeDirectory.findByUsername("noa.b");

        saleService.recordSale(yossi, new RecordSaleRequest("Alice", "1", "050-0000001", CustomerType.NEW, "P1", 3));
        saleService.recordSale(yossi, new RecordSaleRequest("Bob", "2", "050-0000002", CustomerType.RETURNING, "P2", 2));
        saleService.recordSale(noa, new RecordSaleRequest("Carol", "3", "050-0000003", CustomerType.VIP, "P1", 4));
        saleService.recordSale(dana, new RecordSaleRequest("Dave", "4", "050-0000004", CustomerType.NEW, "P3", 1));

        ReportGenerator generator = new ReportGenerator();
        JsonReportExporter jsonExporter = new JsonReportExporter();
        WordReportExporter wordExporter = new WordReportExporter();

        exportBoth(generator.byBranch(saleService.getSalesLedger()), "by-branch", jsonExporter, wordExporter);
        exportBoth(generator.byProduct(saleService.getSalesLedger()), "by-product", jsonExporter, wordExporter);
        exportBoth(generator.byCategory(saleService.getSalesLedger()), "by-category", jsonExporter, wordExporter);

        System.out.println("Reports written to the reports/ directory.");
    }

    private static void exportBoth(SalesReport report, String baseName,
                                    JsonReportExporter jsonExporter, WordReportExporter wordExporter) throws Exception {
        jsonExporter.export(report, Path.of("reports", baseName + ".json"));
        wordExporter.export(report, Path.of("reports", baseName + ".docx"));
    }
}
```

Save as `src/main/java/reports/Main.java`.

- [ ] **Step 2: Manual sanity check (not part of the JUnit suite)**

Run `reports.Main` (from the IDE, or `mvn compile exec:java -Dexec.mainClass=reports.Main`). Confirm it prints `Reports written to the reports/ directory.` and exits. Then check the `reports/` directory: 6 files should exist (`by-branch.json`, `by-branch.docx`, `by-product.json`, `by-product.docx`, `by-category.json`, `by-category.docx`). Open at least one `.docx` file in Word (or LibreOffice/Google Docs) to visually confirm it's a real, readable document with a title and a data table — this is the concrete artifact for the official Word-export requirement.

- [ ] **Step 3: Run the full test suite one final time**

Run: `mvn test`
Expected: `BUILD SUCCESS`, all tests across Stages 1-5 pass.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/reports/Main.java
git commit -m "feat: add reports.Main demo producing real JSON and Word report files"
```

---

## Self-Review Notes

- **Spec coverage:** Comprehensive logging (employee/account/chat via `SystemLogger`, sales via `SaleService`), the chat-content logging toggle, statistical reports grouped by branch/product/category, JSON export (Gson), and real `.docx` Word export (Apache POI, verified by reading the file back) are all present, matching the Stage 5 design spec. The Customer-polymorphism/Inventory gap identified in the spec is closed by `SaleService`.
- **Type consistency:** `Server`'s 4-arg constructor is used consistently everywhere it's built (`Main`, `ServerIntegrationTest`, `ServerConnectionIntegrationTest`, `ChatIntegrationTest`, `SaleIntegrationTest`). `ReportGenerator`'s three grouping methods all return `SalesReport`, consumed identically by both exporters and `reports.Main`.
- **No design pattern introduced beyond the two Singletons** (`ChatDispatcher` from Stage 4, `SystemLogger` here) — both are the textbook-simplest fit for their respective single-shared-instance problems, not pattern-for-pattern's-sake.
