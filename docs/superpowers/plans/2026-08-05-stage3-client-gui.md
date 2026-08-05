# Stage 3 — Client GUI (Swing) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Swing client (login → role/branch dashboard → admin account-creation screen for managers) talking to the real Stage 2 socket server, plus the server-side account-creation support it needs (`PasswordPolicy`, `AccountService`, and a `ClientHandler` that now dispatches more than one message type per connection).

**Architecture:** Server-side: `AccountService` mirrors `AuthService`'s shape — no `Socket`, fully unit-tested, authorized by a `Role` parameter the caller already knows (the connection's logged-in employee). Client-side: `ServerConnection` is a thin blocking wrapper reusing the server's `Serializable` request/response classes directly (client and server share one Maven module). Swing panels (`LoginPanel`/`DashboardPanel`/`AdminPanel`) are wired together by `MainFrame` via `CardLayout`.

**Tech Stack:** Java 17, Maven, JUnit 5, `javax.swing`. No new dependencies.

## Global Constraints

- Academic course project — every line explainable by the student. Plain Swing only (no JavaFX/UI libraries).
- Builds on Stage 1 (`model`) and Stage 2 (`server` — `AuthService`, `EmployeeDirectory`, sockets), both merged to `master`. Design: [2026-08-05-stage3-client-gui-design.md](../specs/2026-08-05-stage3-client-gui-design.md).
- Password policy for admin-created accounts: at least 6 characters and at least one digit — a deliberate default, same status as Stage 1's discount percentages (not a number the lecturer specified).
- The Stage 3 employee dashboard is an information panel (name/role/branch, plus an admin button for managers) — **not** a full purchase/sale screen. That's out of scope here.
- Client and server share `server` package message classes directly (one Maven module, no separate DTO layer) — a documented simplification.
- The two branches (`B1`/`B2`) are hardcoded identically on both client (for the admin form's branch picker) and server (`EmployeeDirectory` seed) — no new "list branches" network call for two fixed values.
- Swing panels (`LoginPanel`, `DashboardPanel`, `AdminPanel`, `MainFrame`) get **no JUnit tests** — documented in the design spec (§6): UI-widget testing needs infrastructure beyond course scope, and everything they call is already unit-tested.
- Out of scope: chat (Stage 4), real persistence (Stage 5), server-push inventory updates.

---

### Task 1: Extend `EmployeeDirectory` (branches + public `addEmployee`)

**Files:**
- Modify: `src/main/java/server/EmployeeDirectory.java`
- Modify: `src/test/java/server/EmployeeDirectoryTest.java`

**Interfaces:**
- Produces: `getBranches()` returning `List<Branch>`, `findBranchById(String)` returning `Branch` or `null`, `addEmployee(Employee)` now `public` (was `private`). Consumed by `AccountService` (Task 3) and the Swing admin form (Task 6).

- [ ] **Step 1: Update the test with the new expectations (failing test)**

Replace the full contents of `src/test/java/server/EmployeeDirectoryTest.java` with:

```java
package server;

import model.Branch;
import model.Employee;
import model.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeDirectoryTest {
    private final EmployeeDirectory directory = new EmployeeDirectory();

    @Test
    void findsSeededEmployeeByUsername() {
        var employee = directory.findByUsername("dana.l");

        assertNotNull(employee);
        assertEquals("Dana Levi", employee.getFullName());
        assertEquals(Role.MANAGER, employee.getRole());
    }

    @Test
    void unknownUsernameReturnsNull() {
        assertNull(directory.findByUsername("nobody"));
    }

    @Test
    void getBranchesReturnsBothSeededBranches() {
        assertEquals(2, directory.getBranches().size());
    }

    @Test
    void findBranchByIdReturnsMatchingBranch() {
        Branch branch = directory.findBranchById("B1");

        assertNotNull(branch);
        assertEquals("Downtown", branch.getName());
    }

    @Test
    void findBranchByIdReturnsNullForUnknownId() {
        assertNull(directory.findBranchById("nope"));
    }

    @Test
    void addEmployeeMakesItFindableByUsername() {
        Branch branch = directory.findBranchById("B1");
        Employee newEmployee = new Employee("100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, branch);

        directory.addEmployee(newEmployee);

        assertEquals(newEmployee, directory.findByUsername("roi.b"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=EmployeeDirectoryTest`
Expected: build failure — no `getBranches()`/`findBranchById(String)`, and `addEmployee` is not visible (private).

- [ ] **Step 3: Update `EmployeeDirectory`**

```java
package server;

import model.Branch;
import model.Employee;
import model.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeDirectory {
    private final Map<String, Employee> employeesByUsername = new HashMap<>();
    private final Map<String, Branch> branchesById = new HashMap<>();

    public EmployeeDirectory() {
        Branch downtown = new Branch("B1", "Downtown", "1 Main St");
        Branch uptown = new Branch("B2", "Uptown", "2 High St");
        branchesById.put(downtown.getBranchId(), downtown);
        branchesById.put(uptown.getBranchId(), uptown);

        addEmployee(new Employee("100000001", "Dana Levi", "dana.l", "secret123", Role.MANAGER, downtown));
        addEmployee(new Employee("100000002", "Yossi Cohen", "yossi.c", "pass456", Role.CASHIER, downtown));
        addEmployee(new Employee("100000003", "Noa Biton", "noa.b", "qwerty789", Role.SELLER, uptown));
    }

    public void addEmployee(Employee employee) {
        employeesByUsername.put(employee.getUsername(), employee);
    }

    public Employee findByUsername(String username) {
        return employeesByUsername.get(username);
    }

    public List<Branch> getBranches() {
        return new ArrayList<>(branchesById.values());
    }

    public Branch findBranchById(String branchId) {
        return branchesById.get(branchId);
    }
}
```

Save as `src/main/java/server/EmployeeDirectory.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=EmployeeDirectoryTest`
Expected: `BUILD SUCCESS`, 6 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/EmployeeDirectory.java src/test/java/server/EmployeeDirectoryTest.java
git commit -m "feat: expose branches and public addEmployee on EmployeeDirectory"
```

---

### Task 2: `server.PasswordPolicy`

**Files:**
- Create: `src/main/java/server/PasswordPolicy.java`
- Test: `src/test/java/server/PasswordPolicyTest.java`

**Interfaces:**
- Produces: `validate(String password)` returning `String` (error message) or `null` (valid). Consumed by `AccountService` (Task 3).

- [ ] **Step 1: Write the failing test**

```java
package server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void validPasswordReturnsNull() {
        assertNull(policy.validate("abcdef1"));
    }

    @Test
    void tooShortPasswordIsRejected() {
        assertNotNull(policy.validate("ab1"));
    }

    @Test
    void passwordWithoutDigitIsRejected() {
        assertNotNull(policy.validate("abcdefgh"));
    }
}
```

Save as `src/test/java/server/PasswordPolicyTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=PasswordPolicyTest`
Expected: build failure — `cannot find symbol: class PasswordPolicy`.

- [ ] **Step 3: Implement `PasswordPolicy`**

```java
package server;

public class PasswordPolicy {
    private static final int MIN_LENGTH = 6;

    public String validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters";
        }
        if (!containsDigit(password)) {
            return "Password must contain at least one digit";
        }
        return null;
    }

    private boolean containsDigit(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }
}
```

Save as `src/main/java/server/PasswordPolicy.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=PasswordPolicyTest`
Expected: `BUILD SUCCESS`, 3 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/PasswordPolicy.java src/test/java/server/PasswordPolicyTest.java
git commit -m "feat: add PasswordPolicy"
```

---

### Task 3: `CreateAccountRequest`/`CreateAccountResponse` + `AccountService`

**Files:**
- Create: `src/main/java/server/CreateAccountRequest.java`
- Create: `src/main/java/server/CreateAccountResponse.java`
- Create: `src/main/java/server/AccountService.java`
- Test: `src/test/java/server/AccountServiceTest.java`

**Interfaces:**
- Consumes: `EmployeeDirectory` (Task 1), `PasswordPolicy` (Task 2).
- Produces: `CreateAccountRequest(String idNumber, String fullName, String username, String password, Role role, String branchId)`. `CreateAccountResponse.success()`, `CreateAccountResponse.failure(String)`, `isSuccess()`, `getErrorMessage()`. `AccountService(EmployeeDirectory, PasswordPolicy)`, `createAccount(Role requesterRole, CreateAccountRequest)` returning `CreateAccountResponse`. Consumed by `ClientHandler` (Task 4).

- [ ] **Step 1: Write the failing test**

```java
package server;

import model.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {
    private final EmployeeDirectory employeeDirectory = new EmployeeDirectory();
    private final AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());

    @Test
    void managerCanCreateAValidAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertTrue(response.isSuccess());
        assertNotNull(employeeDirectory.findByUsername("roi.b"));
    }

    @Test
    void nonManagerCannotCreateAnAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.CASHIER, request);

        assertFalse(response.isSuccess());
        assertNull(employeeDirectory.findByUsername("roi.b"));
    }

    @Test
    void weakPasswordIsRejected() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "weak", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void duplicateUsernameIsRejected() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Second Dana", "dana.l", "abcdef1", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void unknownBranchIsRejected() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B9");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertFalse(response.isSuccess());
    }
}
```

Save as `src/test/java/server/AccountServiceTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=AccountServiceTest`
Expected: build failure — `cannot find symbol: class AccountService` (and `CreateAccountRequest`, `CreateAccountResponse`).

- [ ] **Step 3: Implement `CreateAccountRequest`, `CreateAccountResponse`, `AccountService`**

```java
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
```

Save as `src/main/java/server/CreateAccountRequest.java`.

```java
package server;

import java.io.Serializable;

public class CreateAccountResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;

    private CreateAccountResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static CreateAccountResponse success() {
        return new CreateAccountResponse(true, null);
    }

    public static CreateAccountResponse failure(String errorMessage) {
        return new CreateAccountResponse(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
```

Save as `src/main/java/server/CreateAccountResponse.java`.

```java
package server;

import model.Branch;
import model.Employee;
import model.Role;

public class AccountService {
    private final EmployeeDirectory employeeDirectory;
    private final PasswordPolicy passwordPolicy;

    public AccountService(EmployeeDirectory employeeDirectory, PasswordPolicy passwordPolicy) {
        this.employeeDirectory = employeeDirectory;
        this.passwordPolicy = passwordPolicy;
    }

    public CreateAccountResponse createAccount(Role requesterRole, CreateAccountRequest request) {
        if (requesterRole != Role.MANAGER) {
            return CreateAccountResponse.failure("Only a manager can create accounts");
        }

        String policyError = passwordPolicy.validate(request.getPassword());
        if (policyError != null) {
            return CreateAccountResponse.failure(policyError);
        }

        if (employeeDirectory.findByUsername(request.getUsername()) != null) {
            return CreateAccountResponse.failure("Username already exists");
        }

        Branch branch = employeeDirectory.findBranchById(request.getBranchId());
        if (branch == null) {
            return CreateAccountResponse.failure("Unknown branch");
        }

        Employee newEmployee = new Employee(
                request.getIdNumber(),
                request.getFullName(),
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                branch
        );
        employeeDirectory.addEmployee(newEmployee);

        return CreateAccountResponse.success();
    }
}
```

Save as `src/main/java/server/AccountService.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=AccountServiceTest`
Expected: `BUILD SUCCESS`, 5 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/CreateAccountRequest.java src/main/java/server/CreateAccountResponse.java src/main/java/server/AccountService.java src/test/java/server/AccountServiceTest.java
git commit -m "feat: add AccountService for manager-authorized account creation"
```

---

### Task 4: Wire `AccountService` into `ClientHandler`/`Server`/`Main`

**Files:**
- Modify: `src/main/java/server/ClientHandler.java`
- Modify: `src/main/java/server/Server.java`
- Modify: `src/main/java/server/Main.java`
- Modify: `src/test/java/server/ServerIntegrationTest.java`

**Interfaces:**
- Produces: `Server(int port, AuthService authService, AccountService accountService)` (constructor signature changes — one new parameter). `ClientHandler(Socket, AuthService, AccountService)`.

- [ ] **Step 1: Update the existing integration test to the new constructor (failing test)**

In `src/test/java/server/ServerIntegrationTest.java`, replace the `clientCanLogInOverARealSocket` test body's server setup:

```java
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
```

(Only the first four lines of the method body change — the rest of the file, including the `TEST_PORT`/`server` fields and `tearDown`, stays as-is.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=ServerIntegrationTest`
Expected: build failure — no constructor `Server(int, AuthService, AccountService)`.

- [ ] **Step 3: Update `ClientHandler`, `Server`, `Main`**

```java
package server;

import model.Employee;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService;
    private final AccountService accountService;

    public ClientHandler(Socket socket, AuthService authService, AccountService accountService) {
        this.socket = socket;
        this.authService = authService;
        this.accountService = accountService;
    }

    @Override
    public void run() {
        Employee loggedInEmployee = null;
        try (
                // create the output stream before the input stream on both ends —
                // ObjectInputStream's constructor blocks waiting for the other side's
                // stream header, so mismatched order deadlocks the connection.
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            LoginRequest request = (LoginRequest) in.readObject();
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            out.writeObject(response);
            out.flush();

            if (response.isSuccess()) {
                loggedInEmployee = response.getEmployee();
                handleAuthenticatedSession(in, out, loggedInEmployee);
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

    private void handleAuthenticatedSession(ObjectInputStream in, ObjectOutputStream out, Employee employee)
            throws IOException, ClassNotFoundException {
        // Stage 3 adds account-creation requests here; Stage 4 will add chat
        // message types on this same authenticated connection.
        while (true) {
            Object message = in.readObject();
            if (message instanceof CreateAccountRequest request) {
                CreateAccountResponse response = accountService.createAccount(employee.getRole(), request);
                out.writeObject(response);
                out.flush();
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

Save as `src/main/java/server/ClientHandler.java` (replaces the Stage 2 version).

```java
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;
    private final AuthService authService;
    private final AccountService accountService;
    private ServerSocket serverSocket;

    public Server(int port, AuthService authService, AccountService accountService) {
        this.port = port;
        this.authService = authService;
        this.accountService = accountService;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, authService, accountService)).start();
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
        new Server(5000, authService, accountService).start();
    }
}
```

Save as `src/main/java/server/Main.java` (replaces the Stage 2 version).

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=ServerIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/ClientHandler.java src/main/java/server/Server.java src/main/java/server/Main.java src/test/java/server/ServerIntegrationTest.java
git commit -m "feat: dispatch CreateAccountRequest on authenticated connections"
```

---

### Task 5: `client.ServerConnection` + end-to-end account-creation test

**Files:**
- Create: `src/main/java/client/ServerConnection.java`
- Test: `src/test/java/client/ServerConnectionIntegrationTest.java`

**Interfaces:**
- Consumes: `LoginRequest`/`LoginResponse`/`CreateAccountRequest`/`CreateAccountResponse` (server package), `Server`/`AuthService`/`AccountService`/`EmployeeDirectory`/`PasswordPolicy` (server package, for the test's server setup).
- Produces: `ServerConnection(String host, int port)`, `login(String, String)` returning `LoginResponse`, `createAccount(CreateAccountRequest)` returning `CreateAccountResponse`, `close()`. Consumed by the Swing panels in Task 6.

- [ ] **Step 1: Write the failing test**

```java
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
```

Save as `src/test/java/client/ServerConnectionIntegrationTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=ServerConnectionIntegrationTest`
Expected: build failure — `cannot find symbol: class ServerConnection`.

- [ ] **Step 3: Implement `ServerConnection`**

```java
package client;

import server.CreateAccountRequest;
import server.CreateAccountResponse;
import server.LoginRequest;
import server.LoginResponse;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection implements Closeable {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public ServerConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // create the output stream before the input stream on both ends —
        // ObjectInputStream's constructor blocks waiting for the other side's
        // stream header, so mismatched order deadlocks the connection.
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public LoginResponse login(String username, String password) throws IOException, ClassNotFoundException {
        out.writeObject(new LoginRequest(username, password));
        out.flush();
        return (LoginResponse) in.readObject();
    }

    public CreateAccountResponse createAccount(CreateAccountRequest request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (CreateAccountResponse) in.readObject();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
```

Save as `src/main/java/client/ServerConnection.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=ServerConnectionIntegrationTest`
Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

- [ ] **Step 5: Run the full test suite**

Run: `mvn test`
Expected: `BUILD SUCCESS`. All Stage 1/2/3 tests pass — 35 tests total.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/client/ServerConnection.java src/test/java/client/ServerConnectionIntegrationTest.java
git commit -m "feat: add client.ServerConnection with an end-to-end account-creation test"
```

---

### Task 6: Swing screens (`LoginPanel`, `DashboardPanel`, `AdminPanel`, `MainFrame`, `client.Main`)

**Files:**
- Modify: `src/main/java/model/Branch.java` (add `toString()` so `JComboBox<Branch>` displays branch names, not `Branch@hashcode`)
- Create: `src/main/java/client/LoginPanel.java`
- Create: `src/main/java/client/DashboardPanel.java`
- Create: `src/main/java/client/AdminPanel.java`
- Create: `src/main/java/client/MainFrame.java`
- Create: `src/main/java/client/Main.java`

**Interfaces:**
- Consumes: `ServerConnection` (Task 5), `Employee`/`Role`/`Branch` (Stage 1), `CreateAccountRequest`/`CreateAccountResponse`/`LoginResponse` (Stage 2/3).
- No new interfaces are produced for later tasks — this is the last task in the plan.

No JUnit test for this task (see Global Constraints and design spec §6) — verified by the manual check in Step 6.

- [ ] **Step 1: Add `toString()` to `Branch`**

In `src/main/java/model/Branch.java`, add inside the class body:

```java
    @Override
    public String toString() {
        return name;
    }
```

- [ ] **Step 2: Implement `LoginPanel`**

```java
package client;

import model.Employee;
import server.LoginResponse;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private final ServerConnection connection;
    private final Consumer<Employee> onLoginSuccess;

    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JLabel errorLabel = new JLabel(" ");

    public LoginPanel(ServerConnection connection, Consumer<Employee> onLoginSuccess) {
        this.connection = connection;
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel("שם משתמש:"), c);
        c.gridx = 1;
        add(usernameField, c);

        c.gridx = 0;
        c.gridy = 1;
        add(new JLabel("סיסמה:"), c);
        c.gridx = 1;
        add(passwordField, c);

        JButton loginButton = new JButton("התחבר");
        loginButton.addActionListener(e -> attemptLogin());
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        add(loginButton, c);

        errorLabel.setForeground(Color.RED);
        c.gridy = 3;
        add(errorLabel, c);
    }

    private void attemptLogin() {
        try {
            LoginResponse response = connection.login(usernameField.getText(), new String(passwordField.getPassword()));
            if (response.isSuccess()) {
                errorLabel.setText(" ");
                onLoginSuccess.accept(response.getEmployee());
            } else {
                errorLabel.setText(response.getErrorMessage());
            }
        } catch (Exception e) {
            errorLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }
}
```

Save as `src/main/java/client/LoginPanel.java`.

- [ ] **Step 3: Implement `DashboardPanel`**

```java
package client;

import model.Employee;
import model.Role;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    public DashboardPanel(Employee employee, Runnable onOpenAdmin) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;

        c.gridy = 0;
        add(new JLabel("שלום, " + employee.getFullName()), c);
        c.gridy = 1;
        add(new JLabel("תפקיד: " + employee.getRole()), c);
        c.gridy = 2;
        add(new JLabel("סניף: " + employee.getBranch().getName()), c);

        if (employee.getRole() == Role.MANAGER) {
            JButton adminButton = new JButton("ניהול חשבונות");
            adminButton.addActionListener(e -> onOpenAdmin.run());
            c.gridy = 3;
            add(adminButton, c);
        }
    }
}
```

Save as `src/main/java/client/DashboardPanel.java`.

- [ ] **Step 4: Implement `AdminPanel`**

```java
package client;

import model.Branch;
import model.Role;
import server.CreateAccountRequest;
import server.CreateAccountResponse;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdminPanel extends JPanel {
    private final ServerConnection connection;

    private final JTextField idNumberField = new JTextField(15);
    private final JTextField fullNameField = new JTextField(15);
    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JComboBox<Role> roleBox = new JComboBox<>(Role.values());
    private final JComboBox<Branch> branchBox;
    private final JLabel statusLabel = new JLabel(" ");

    public AdminPanel(ServerConnection connection, List<Branch> branches) {
        this.connection = connection;
        this.branchBox = new JComboBox<>(branches.toArray(new Branch[0]));

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        addRow(c, 0, "ת\"ז:", idNumberField);
        addRow(c, 1, "שם מלא:", fullNameField);
        addRow(c, 2, "שם משתמש:", usernameField);
        addRow(c, 3, "סיסמה:", passwordField);
        addRow(c, 4, "תפקיד:", roleBox);
        addRow(c, 5, "סניף:", branchBox);

        JButton createButton = new JButton("צור חשבון");
        createButton.addActionListener(e -> attemptCreateAccount());
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        add(createButton, c);

        statusLabel.setForeground(Color.RED);
        c.gridy = 7;
        add(statusLabel, c);
    }

    private void addRow(GridBagConstraints c, int row, String label, Component field) {
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = row;
        add(new JLabel(label), c);
        c.gridx = 1;
        add(field, c);
    }

    private void attemptCreateAccount() {
        try {
            Branch selectedBranch = (Branch) branchBox.getSelectedItem();
            CreateAccountRequest request = new CreateAccountRequest(
                    idNumberField.getText(),
                    fullNameField.getText(),
                    usernameField.getText(),
                    new String(passwordField.getPassword()),
                    (Role) roleBox.getSelectedItem(),
                    selectedBranch.getBranchId()
            );

            CreateAccountResponse response = connection.createAccount(request);
            statusLabel.setForeground(response.isSuccess() ? new Color(0, 128, 0) : Color.RED);
            statusLabel.setText(response.isSuccess() ? "החשבון נוצר בהצלחה" : response.getErrorMessage());
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }
}
```

Save as `src/main/java/client/AdminPanel.java`.

- [ ] **Step 5: Implement `MainFrame` and `client.Main`**

```java
package client;

import model.Branch;
import model.Employee;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    // Hardcoded to match server.EmployeeDirectory's seed data — see the
    // Stage 3 design spec for why the client doesn't fetch this over the wire.
    private static final List<Branch> BRANCHES = List.of(
            new Branch("B1", "Downtown", "1 Main St"),
            new Branch("B2", "Uptown", "2 High St")
    );

    private static final String LOGIN_CARD = "login";
    private static final String DASHBOARD_CARD = "dashboard";
    private static final String ADMIN_CARD = "admin";

    private final ServerConnection connection;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public MainFrame(ServerConnection connection) {
        super("מערכת ניהול רשת חנויות");
        this.connection = connection;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        cards.add(new LoginPanel(connection, this::showDashboard), LOGIN_CARD);
        add(cards);

        cardLayout.show(cards, LOGIN_CARD);
    }

    private void showDashboard(Employee employee) {
        cards.add(new DashboardPanel(employee, () -> showAdmin(employee)), DASHBOARD_CARD);
        cardLayout.show(cards, DASHBOARD_CARD);
    }

    private void showAdmin(Employee employee) {
        cards.add(new AdminPanel(connection, BRANCHES), ADMIN_CARD);
        cardLayout.show(cards, ADMIN_CARD);
    }
}
```

Save as `src/main/java/client/MainFrame.java`.

```java
package client;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) throws Exception {
        ServerConnection connection = new ServerConnection("localhost", 5000);
        SwingUtilities.invokeLater(() -> new MainFrame(connection).setVisible(true));
    }
}
```

Save as `src/main/java/client/Main.java`.

- [ ] **Step 6: Manual sanity check (not part of the JUnit suite)**

1. Run `server.Main` (listens on port 5000).
2. Run `client.Main` — a window opens. Log in as `dana.l` / `secret123` (seeded manager). Confirm the dashboard shows her name/role/branch and a "ניהול חשבונות" button.
3. Click it, fill in the form (any new username/password meeting the policy, e.g. username `test.s`, password `abcdef1`), submit. Confirm the success message.
4. Run a second `client.Main`, log in with the new username/password, confirm the dashboard appears **without** the admin button (role is not `MANAGER`).
5. Run `mvn test` one more time to confirm nothing broke: `BUILD SUCCESS`, 35 tests.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/model/Branch.java src/main/java/client/LoginPanel.java src/main/java/client/DashboardPanel.java src/main/java/client/AdminPanel.java src/main/java/client/MainFrame.java src/main/java/client/Main.java
git commit -m "feat: add Swing login/dashboard/admin screens"
```

---

## Self-Review Notes

- **Spec coverage:** Login screen, admin account-creation screen (manager-only, password policy enforced), and role/branch-adapted dashboard are all present, matching the Stage 3 design spec. The documented out-of-scope items (full purchase UI, server-push inventory, chat) are not touched.
- **Type consistency:** `AccountService.createAccount(Role, CreateAccountRequest)` signature matches every caller (`AccountServiceTest`, `ClientHandler`). `ServerConnection.login`/`createAccount` return types match what `LoginPanel`/`AdminPanel` expect. `Server`'s 3-arg constructor is used consistently everywhere it's constructed (`Main`, `ServerIntegrationTest`, `ServerConnectionIntegrationTest`).
- **No design pattern introduced** — `instanceof` dispatch in `ClientHandler` is plain Java, not a Command/Visitor pattern; Design Patterns stay reserved for Stage 4.
