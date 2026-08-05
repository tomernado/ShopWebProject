# Stage 2 — Server (Threads + Authentication) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Socket server that accepts multiple clients (one thread each) and authenticates logins, rejecting a second simultaneous login for the same username, with the authentication logic fully unit-tested independent of sockets.

**Architecture:** `server.AuthService` holds all login/logout logic and a thread-safe set of active usernames — no `Socket` in sight, fully JUnit-testable. `server.ClientHandler` (one per accepted connection, run on its own `Thread`) is a thin adapter: read a `LoginRequest` off the wire, call `AuthService`, write back a `LoginResponse`. `server.Server` owns the accept loop and has `start()`/`stop()` so tests can run it in the background and shut it down.

**Tech Stack:** Java 17, Maven, JUnit 5 — same as Stage 1. `java.net.Socket`/`ServerSocket` and `java.util.concurrent.ConcurrentHashMap` from the standard library only.

## Global Constraints

- Academic course project — every line must be explainable in plain terms by the student. No frameworks, no networking libraries beyond `java.net`, no thread pools/executors — plain `new Thread(...).start()` per client, matching the course's own wording ("ניהול Threads מול ריבוי לקוחות").
- Builds on Stage 1's merged `model` package (`Employee`, `Branch`, `Role`) — see [2026-08-05-stage1-entities-design.md](../specs/2026-08-05-stage1-entities-design.md).
- Design per [2026-08-05-stage2-server-design.md](../specs/2026-08-05-stage2-server-design.md).
- Passwords are compared as plain strings — a deliberate, documented course-level simplification, not an oversight. No hashing.
- A client that disconnects without a clean logout leaves its username "active" until the socket read fails — accepted limitation, no heartbeat/timeout mechanism.
- Out of scope for this stage: any GUI (admin account screen, password policy screen — Stage 3), chat message types beyond login (Stage 4), real persistence for employees (Stage 5, `EmployeeDirectory` stays in-memory until then).

---

### Task 1: Add `password` to `Employee`; make `Employee`/`Branch` serializable

**Files:**
- Modify: `src/main/java/model/Employee.java`
- Modify: `src/main/java/model/Branch.java`
- Modify: `src/test/java/model/EmployeeTest.java`

**Interfaces:**
- Produces: `Employee(String idNumber, String fullName, String username, String password, Role role, Branch branch)` (constructor signature changes — one new parameter before `role`), new `getPassword()`. `Employee` and `Branch` now `implements Serializable`.

- [ ] **Step 1: Update the test to the new constructor signature (failing test)**

Replace the full contents of `src/test/java/model/EmployeeTest.java` with:

```java
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeTest {
    @Test
    void constructorStoresAllFields() {
        Branch branch = new Branch("B1", "Downtown", "1 Main St");
        Employee employee = new Employee("123456789", "Dana Levi", "dana.l", "secret123", Role.CASHIER, branch);

        assertEquals("123456789", employee.getIdNumber());
        assertEquals("Dana Levi", employee.getFullName());
        assertEquals("dana.l", employee.getUsername());
        assertEquals("secret123", employee.getPassword());
        assertEquals(Role.CASHIER, employee.getRole());
        assertEquals(branch, employee.getBranch());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=EmployeeTest`
Expected: build failure — no constructor `Employee(String,String,String,String,Role,Branch)` and no `getPassword()`.

- [ ] **Step 3: Update `Employee` and `Branch`**

```java
package model;

import java.io.Serializable;

public class Employee implements Serializable {
    private final String idNumber;
    private final String fullName;
    private final String username;
    private final String password;
    private final Role role;
    private final Branch branch;

    public Employee(String idNumber, String fullName, String username, String password, Role role, Branch branch) {
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.branch = branch;
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

    public Branch getBranch() {
        return branch;
    }
}
```

Save as `src/main/java/model/Employee.java` (replaces the Stage 1 version).

In `src/main/java/model/Branch.java`, add the import and `implements Serializable`:

```java
package model;

import java.io.Serializable;

public class Branch implements Serializable {
    private final String branchId;
    private final String name;
    private final String address;

    public Branch(String branchId, String name, String address) {
        this.branchId = branchId;
        this.name = name;
        this.address = address;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=EmployeeTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 5: Run the full suite to confirm nothing else broke**

Run: `mvn test`
Expected: `BUILD SUCCESS`, all 13 existing Stage 1 tests still pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/model/Employee.java src/main/java/model/Branch.java src/test/java/model/EmployeeTest.java
git commit -m "feat: add password to Employee, make Employee/Branch serializable"
```

---

### Task 2: `server.EmployeeDirectory` (seed data)

**Files:**
- Create: `src/main/java/server/EmployeeDirectory.java`
- Test: `src/test/java/server/EmployeeDirectoryTest.java`

**Interfaces:**
- Consumes: `Employee`, `Branch`, `Role` from Task 1 / Stage 1.
- Produces: `EmployeeDirectory()` (no-arg constructor, self-seeds), `findByUsername(String username)` returning `Employee` or `null`. Consumed by `AuthService` in Task 3.

- [ ] **Step 1: Write the failing test**

```java
package server;

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
}
```

Save as `src/test/java/server/EmployeeDirectoryTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=EmployeeDirectoryTest`
Expected: build failure — `cannot find symbol: class EmployeeDirectory`.

- [ ] **Step 3: Implement `EmployeeDirectory`**

```java
package server;

import model.Branch;
import model.Employee;
import model.Role;

import java.util.HashMap;
import java.util.Map;

public class EmployeeDirectory {
    private final Map<String, Employee> employeesByUsername = new HashMap<>();

    public EmployeeDirectory() {
        Branch downtown = new Branch("B1", "Downtown", "1 Main St");
        Branch uptown = new Branch("B2", "Uptown", "2 High St");

        addEmployee(new Employee("100000001", "Dana Levi", "dana.l", "secret123", Role.MANAGER, downtown));
        addEmployee(new Employee("100000002", "Yossi Cohen", "yossi.c", "pass456", Role.CASHIER, downtown));
        addEmployee(new Employee("100000003", "Noa Biton", "noa.b", "qwerty789", Role.SELLER, uptown));
    }

    private void addEmployee(Employee employee) {
        employeesByUsername.put(employee.getUsername(), employee);
    }

    public Employee findByUsername(String username) {
        return employeesByUsername.get(username);
    }
}
```

Save as `src/main/java/server/EmployeeDirectory.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=EmployeeDirectoryTest`
Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/EmployeeDirectory.java src/test/java/server/EmployeeDirectoryTest.java
git commit -m "feat: add EmployeeDirectory with in-memory seed data"
```

---

### Task 3: `server.LoginResponse` + `server.AuthService`

**Files:**
- Create: `src/main/java/server/LoginResponse.java`
- Create: `src/main/java/server/AuthService.java`
- Test: `src/test/java/server/AuthServiceTest.java`

**Interfaces:**
- Consumes: `EmployeeDirectory` from Task 2.
- Produces: `LoginResponse.success(Employee)`, `LoginResponse.failure(String)`, `isSuccess()`, `getErrorMessage()`, `getEmployee()`. `AuthService(EmployeeDirectory)`, `login(String username, String password)` returning `LoginResponse`, `logout(String username)`. Consumed by `ClientHandler` in Task 4.

- [ ] **Step 1: Write the failing test**

```java
package server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private final AuthService authService = new AuthService(new EmployeeDirectory());

    @Test
    void validCredentialsLogInSuccessfully() {
        LoginResponse response = authService.login("dana.l", "secret123");

        assertTrue(response.isSuccess());
        assertEquals("Dana Levi", response.getEmployee().getFullName());
    }

    @Test
    void wrongPasswordFails() {
        LoginResponse response = authService.login("dana.l", "wrongpassword");

        assertFalse(response.isSuccess());
        assertNull(response.getEmployee());
    }

    @Test
    void unknownUsernameFails() {
        LoginResponse response = authService.login("nobody", "whatever");

        assertFalse(response.isSuccess());
    }

    @Test
    void secondSimultaneousLoginForSameUserFails() {
        authService.login("dana.l", "secret123");

        LoginResponse response = authService.login("dana.l", "secret123");

        assertFalse(response.isSuccess());
    }

    @Test
    void loginSucceedsAgainAfterLogout() {
        authService.login("dana.l", "secret123");
        authService.logout("dana.l");

        LoginResponse response = authService.login("dana.l", "secret123");

        assertTrue(response.isSuccess());
    }
}
```

Save as `src/test/java/server/AuthServiceTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=AuthServiceTest`
Expected: build failure — `cannot find symbol: class AuthService` (and `LoginResponse`).

- [ ] **Step 3: Implement `LoginResponse` and `AuthService`**

```java
package server;

import model.Employee;
import java.io.Serializable;

public class LoginResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;
    private final Employee employee;

    private LoginResponse(boolean success, String errorMessage, Employee employee) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.employee = employee;
    }

    public static LoginResponse success(Employee employee) {
        return new LoginResponse(true, null, employee);
    }

    public static LoginResponse failure(String errorMessage) {
        return new LoginResponse(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Employee getEmployee() {
        return employee;
    }
}
```

Save as `src/main/java/server/LoginResponse.java`.

```java
package server;

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

        return LoginResponse.success(employee);
    }

    public void logout(String username) {
        activeSessions.remove(username);
    }
}
```

Save as `src/main/java/server/AuthService.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=AuthServiceTest`
Expected: `BUILD SUCCESS`, 5 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/LoginResponse.java src/main/java/server/AuthService.java src/test/java/server/AuthServiceTest.java
git commit -m "feat: add AuthService with duplicate-login prevention"
```

---

### Task 4: `server.LoginRequest` + `ClientHandler` + `Server` + `Main` (socket layer)

**Files:**
- Create: `src/main/java/server/LoginRequest.java`
- Create: `src/main/java/server/ClientHandler.java`
- Create: `src/main/java/server/Server.java`
- Create: `src/main/java/server/Main.java`
- Test: `src/test/java/server/ServerIntegrationTest.java`

**Interfaces:**
- Consumes: `AuthService`, `LoginResponse` from Task 3; `EmployeeDirectory` from Task 2.
- Produces: `LoginRequest(String username, String password)`. `Server(int port, AuthService authService)` with `start()` (blocks, accepts connections until `stop()` is called) and `stop()`. `Main.main(String[])` — never called from tests.

- [ ] **Step 1: Write the failing integration test**

```java
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
        server = new Server(TEST_PORT, new AuthService(new EmployeeDirectory()));
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
```

Save as `src/test/java/server/ServerIntegrationTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=ServerIntegrationTest`
Expected: build failure — `cannot find symbol: class Server` (and `LoginRequest`).

- [ ] **Step 3: Implement `LoginRequest`, `ClientHandler`, `Server`, `Main`**

```java
package server;

import java.io.Serializable;

public class LoginRequest implements Serializable {
    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
```

Save as `src/main/java/server/LoginRequest.java`.

```java
package server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService;

    public ClientHandler(Socket socket, AuthService authService) {
        this.socket = socket;
        this.authService = authService;
    }

    @Override
    public void run() {
        String loggedInUsername = null;
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
                loggedInUsername = request.getUsername();
                waitForDisconnect(in);
            }
        } catch (EOFException e) {
            // client disconnected — normal end of the read loop
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            if (loggedInUsername != null) {
                authService.logout(loggedInUsername);
            }
            closeSocket();
        }
    }

    private void waitForDisconnect(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Stage 2 only authenticates; later stages will read further message
        // types here on the same connection (e.g. chat in Stage 4).
        while (true) {
            in.readObject();
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

Save as `src/main/java/server/ClientHandler.java`.

```java
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;
    private final AuthService authService;
    private ServerSocket serverSocket;

    public Server(int port, AuthService authService) {
        this.port = port;
        this.authService = authService;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, authService)).start();
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

Save as `src/main/java/server/Server.java`.

```java
package server;

public class Main {
    public static void main(String[] args) {
        new Server(5000, new AuthService(new EmployeeDirectory())).start();
    }
}
```

Save as `src/main/java/server/Main.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=ServerIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 5: Run the full test suite**

Run: `mvn test`
Expected: `BUILD SUCCESS`. All tests across Stage 1 (`BranchTest`, `ProductTest`, `EmployeeTest`, `InventoryTest`, `CustomerDiscountTest`) and Stage 2 (`EmployeeDirectoryTest`, `AuthServiceTest`, `ServerIntegrationTest`) pass — 22 tests total.

- [ ] **Step 6: Manual sanity check (not part of the JUnit suite)**

Run `mvn compile exec:java -Dexec.mainClass=server.Main` (or run `Main` from the IDE) in one terminal, confirm it doesn't exit immediately (it's listening on port 5000). Stop it with Ctrl+C. This isn't automated — it's a one-time human check that the entry point actually runs, per the Stage 2 Definition of Done.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/server/LoginRequest.java src/main/java/server/ClientHandler.java src/main/java/server/Server.java src/main/java/server/Main.java src/test/java/server/ServerIntegrationTest.java
git commit -m "feat: add ClientHandler, Server, and Main for socket-based login"
```

---

## Self-Review Notes

- **Spec coverage:** Multi-client threading (one `Thread` per accepted connection in `Server.start()`), authentication (`AuthService`), duplicate-login prevention (`activeSessions` set with atomic `add`), and the documented simplifications (plain-string passwords, no stale-session timeout) are all present, matching the Stage 2 design spec.
- **Type consistency:** `AuthService.login` returns `LoginResponse` everywhere it's used (`AuthServiceTest`, `ClientHandler`); `EmployeeDirectory.findByUsername` returns `Employee` consistently; `LoginRequest`/`LoginResponse` field names match between `ClientHandler` and `ServerIntegrationTest`.
- **No design pattern introduced** — matches the constraint that Design Patterns are reserved for the Stage 4 chat requirement.
