# Purchase GUI Gap Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close a gap found during the post-Stage-5 requirements review: the spec asks for "a purchase/sale interface that displays existing inventory," but `SaleService` (Stage 5) was only reachable over the raw protocol — no Swing screen existed. This adds a real `PurchasePanel` backed by a new inventory-query operation.

**Architecture:** A new query/response pair (`GetInventoryRequest`/`GetInventoryResponse`, carrying a projection DTO `InventoryItem` — matching how `ChatSummary` already projects `ChatSession` internals rather than leaking domain objects over the wire) lets the client ask "what's in stock at my branch." `SaleService` gains a read-only `getInventorySnapshot(Employee)` method alongside its existing `recordSale`. `ClientHandler` gains one more `instanceof` branch, following the exact shape used for every other request type since Stage 3. `client.ServerConnection` gains `getInventory()`/`recordSale()`, both synchronous request/response like `login()`/`createAccount()` — no async-push complexity needed here, unlike chat.

**Tech Stack:** No new dependencies — same Java 17 / Maven / JUnit 5 stack.

## Global Constraints

- Same course-level constraints as every prior stage: simple, explainable code, no unnecessary abstraction.
- `SaleService.recordSale` still has no role restriction (any logged-in employee can record a sale) — the Purchase button is visible to every role, consistent with that.
- Builds on `master` (all 5 stages merged). No changes to `AuthService`, `AccountService`, `ChatDispatcher`, or any wire type other than the new ones listed here.

---

### Task 1: `InventoryItem` DTO + `ProductCatalog.getAllProducts()` + `SaleService.getInventorySnapshot()`

**Files:**
- Create: `src/main/java/server/InventoryItem.java`
- Modify: `src/main/java/server/ProductCatalog.java`
- Modify: `src/main/java/server/SaleService.java`
- Modify: `src/test/java/server/ProductCatalogTest.java`
- Modify: `src/test/java/server/SaleServiceTest.java`

**Interfaces:**
- Produces: `InventoryItem(productId, productName, category, price, availableQuantity)` with getters. `ProductCatalog.getAllProducts()` returning `Collection<Product>`. `SaleService.getInventorySnapshot(Employee)` returning `List<InventoryItem>`. Consumed by `ClientHandler` (Task 2).

- [ ] **Step 1: Add the failing tests**

Add to `src/test/java/server/ProductCatalogTest.java`:

```java

    @Test
    void getAllProductsReturnsAllThreeSeededProducts() {
        assertEquals(3, catalog.getAllProducts().size());
    }
```

Add to `src/test/java/server/SaleServiceTest.java` (needs `import model.CustomerType;` already present):

```java

    @Test
    void inventorySnapshotReflectsCurrentStockForEmployeesBranch() {
        var snapshot = saleService.getInventorySnapshot(cashier);

        assertEquals(3, snapshot.size());
        var milk = snapshot.stream().filter(item -> item.getProductId().equals("P1")).findFirst().orElseThrow();
        assertEquals(50, milk.getAvailableQuantity());
    }

    @Test
    void inventorySnapshotReflectsStockAfterASale() {
        saleService.recordSale(cashier, new RecordSaleRequest("A", "1", "050", CustomerType.NEW, "P1", 5));

        var snapshot = saleService.getInventorySnapshot(cashier);
        var milk = snapshot.stream().filter(item -> item.getProductId().equals("P1")).findFirst().orElseThrow();

        assertEquals(45, milk.getAvailableQuantity());
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn test -Dtest=ProductCatalogTest,SaleServiceTest`
Expected: build failure — no `getAllProducts()`, no `getInventorySnapshot(...)`, no `InventoryItem`.

- [ ] **Step 3: Implement `InventoryItem`, `ProductCatalog.getAllProducts()`, `SaleService.getInventorySnapshot()`**

```java
package server;

import java.io.Serializable;

public class InventoryItem implements Serializable {
    private final String productId;
    private final String productName;
    private final String category;
    private final double price;
    private final int availableQuantity;

    public InventoryItem(String productId, String productName, String category, double price, int availableQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.price = price;
        this.availableQuantity = availableQuantity;
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

    public double getPrice() {
        return price;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
```

Save as `src/main/java/server/InventoryItem.java`.

In `src/main/java/server/ProductCatalog.java`, add the import `import java.util.Collection;` and this method:

```java
    public Collection<Product> getAllProducts() {
        return productsById.values();
    }
```

In `src/main/java/server/SaleService.java`, add this method (after `recordSale`, before `buildCustomer`):

```java
    public synchronized List<InventoryItem> getInventorySnapshot(Employee employee) {
        Inventory inventory = productCatalog.getInventoryForBranch(employee.getBranch().getBranchId());
        List<InventoryItem> items = new ArrayList<>();
        for (Product product : productCatalog.getAllProducts()) {
            int quantity = inventory != null ? inventory.getQuantity(product) : 0;
            items.add(new InventoryItem(product.getProductId(), product.getName(), product.getCategory(),
                    product.getPrice(), quantity));
        }
        return items;
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn test -Dtest=ProductCatalogTest,SaleServiceTest`
Expected: `BUILD SUCCESS`, 5 + 7 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/InventoryItem.java src/main/java/server/ProductCatalog.java src/main/java/server/SaleService.java src/test/java/server/ProductCatalogTest.java src/test/java/server/SaleServiceTest.java
git commit -m "feat: add inventory snapshot query to SaleService"
```

---

### Task 2: `GetInventoryRequest`/`GetInventoryResponse` + wire into `ClientHandler` + end-to-end test

**Files:**
- Create: `src/main/java/server/GetInventoryRequest.java`
- Create: `src/main/java/server/GetInventoryResponse.java`
- Modify: `src/main/java/server/ClientHandler.java`
- Modify: `src/test/java/server/SaleIntegrationTest.java`

**Interfaces:**
- Produces: `GetInventoryRequest()` (marker), `GetInventoryResponse(List<InventoryItem>)`. Consumed by `client.ServerConnection` (Task 3).

- [ ] **Step 1: Add the failing test**

Add to `src/test/java/server/SaleIntegrationTest.java` (same class, second `@Test` method):

```java

    @Test
    void inventoryReflectsSaleWhenFetchedAgain() throws Exception {
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
```

(`CustomerType` is already imported in this file from the first test.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=SaleIntegrationTest`
Expected: build failure — no `GetInventoryRequest`/`GetInventoryResponse` classes.

- [ ] **Step 3: Implement `GetInventoryRequest`, `GetInventoryResponse`, and wire into `ClientHandler`**

```java
package server;

import java.io.Serializable;

public class GetInventoryRequest implements Serializable {
}
```

Save as `src/main/java/server/GetInventoryRequest.java`.

```java
package server;

import java.io.Serializable;
import java.util.List;

public class GetInventoryResponse implements Serializable {
    private final List<InventoryItem> items;

    public GetInventoryResponse(List<InventoryItem> items) {
        this.items = items;
    }

    public List<InventoryItem> getItems() {
        return items;
    }
}
```

Save as `src/main/java/server/GetInventoryResponse.java`.

In `src/main/java/server/ClientHandler.java`, add one more branch to `handleAuthenticatedSession`'s `if`/`else if` chain, right after the `RecordSaleRequest` branch:

```java
            } else if (message instanceof GetInventoryRequest) {
                send(new GetInventoryResponse(saleService.getInventorySnapshot(loggedInEmployee)));
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=SaleIntegrationTest`
Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/server/GetInventoryRequest.java src/main/java/server/GetInventoryResponse.java src/main/java/server/ClientHandler.java src/test/java/server/SaleIntegrationTest.java
git commit -m "feat: dispatch GetInventoryRequest on authenticated connections"
```

---

### Task 3: `client.ServerConnection` gains `getInventory()`/`recordSale()`

**Files:**
- Modify: `src/main/java/client/ServerConnection.java`

**Interfaces:**
- Produces: `recordSale(RecordSaleRequest)` returning `RecordSaleResponse`, `getInventory()` returning `GetInventoryResponse`. Consumed by `PurchasePanel` (Task 4).

No new test for this task — it's two one-line methods with the exact same shape as the already-tested `login`/`createAccount`, and it gets exercised for real by the Swing panel in Task 4's manual check.

- [ ] **Step 1: Add the two methods**

In `src/main/java/client/ServerConnection.java`, add these imports:

```java
import server.GetInventoryRequest;
import server.GetInventoryResponse;
import server.RecordSaleRequest;
import server.RecordSaleResponse;
```

And these methods (after `createAccount`):

```java
    public RecordSaleResponse recordSale(RecordSaleRequest request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (RecordSaleResponse) in.readObject();
    }

    public GetInventoryResponse getInventory() throws IOException, ClassNotFoundException {
        out.writeObject(new GetInventoryRequest());
        out.flush();
        return (GetInventoryResponse) in.readObject();
    }
```

- [ ] **Step 2: Run the full suite to confirm nothing broke**

Run: `mvn test`
Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/client/ServerConnection.java
git commit -m "feat: add recordSale and getInventory to ServerConnection"
```

---

### Task 4: `PurchasePanel` + wire into `DashboardPanel`/`MainFrame`

**Files:**
- Create: `src/main/java/client/PurchasePanel.java`
- Modify: `src/main/java/client/DashboardPanel.java`
- Modify: `src/main/java/client/MainFrame.java`

**Interfaces:**
- `DashboardPanel`'s constructor gains a third parameter (`Runnable onOpenPurchase`) — its only caller, `MainFrame`, is updated in this same task.

No JUnit test for this task — same documented Swing-testing boundary as every other screen since Stage 3 (`LoginPanel`, `AdminPanel`). Verified by the manual check in Step 4.

- [ ] **Step 1: Implement `PurchasePanel`**

```java
package client;

import model.CustomerType;
import server.GetInventoryResponse;
import server.InventoryItem;
import server.RecordSaleRequest;
import server.RecordSaleResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PurchasePanel extends JPanel {
    private final ServerConnection connection;

    private final DefaultTableModel inventoryTableModel =
            new DefaultTableModel(new Object[]{"מוצר", "קטגוריה", "מחיר", "במלאי"}, 0);
    private final JTable inventoryTable = new JTable(inventoryTableModel);

    private final JTextField productIdField = new JTextField(10);
    private final JTextField quantityField = new JTextField(5);
    private final JTextField customerNameField = new JTextField(15);
    private final JTextField customerIdField = new JTextField(15);
    private final JTextField customerPhoneField = new JTextField(15);
    private final JComboBox<CustomerType> customerTypeBox = new JComboBox<>(CustomerType.values());
    private final JLabel statusLabel = new JLabel(" ");

    public PurchasePanel(ServerConnection connection) {
        this.connection = connection;

        setLayout(new BorderLayout(10, 10));
        add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        // Clicking a row fills in its product id, so the operator doesn't have
        // to retype it — a small usability win, not a new mechanism.
        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            int row = inventoryTable.getSelectedRow();
            if (row >= 0) {
                String cell = (String) inventoryTableModel.getValueAt(row, 0);
                productIdField.setText(cell.split(" - ")[0]);
            }
        });

        refreshInventory();
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);

        addRow(form, c, 0, "מזהה מוצר:", productIdField);
        addRow(form, c, 1, "כמות:", quantityField);
        addRow(form, c, 2, "שם לקוח:", customerNameField);
        addRow(form, c, 3, "ת\"ז לקוח:", customerIdField);
        addRow(form, c, 4, "טלפון לקוח:", customerPhoneField);
        addRow(form, c, 5, "סוג לקוח:", customerTypeBox);

        JButton buyButton = new JButton("בצע רכישה");
        buyButton.addActionListener(e -> attemptPurchase());
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        form.add(buyButton, c);

        statusLabel.setForeground(Color.RED);
        c.gridy = 7;
        form.add(statusLabel, c);

        return form;
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, Component field) {
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = row;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
    }

    private void refreshInventory() {
        try {
            GetInventoryResponse response = connection.getInventory();
            inventoryTableModel.setRowCount(0);
            for (InventoryItem item : response.getItems()) {
                inventoryTableModel.addRow(new Object[]{
                        item.getProductId() + " - " + item.getProductName(),
                        item.getCategory(),
                        String.format("%.2f", item.getPrice()),
                        item.getAvailableQuantity()
                });
            }
        } catch (Exception e) {
            statusLabel.setText("שגיאה בטעינת המלאי: " + e.getMessage());
        }
    }

    private void attemptPurchase() {
        try {
            RecordSaleRequest request = new RecordSaleRequest(
                    customerNameField.getText(),
                    customerIdField.getText(),
                    customerPhoneField.getText(),
                    (CustomerType) customerTypeBox.getSelectedItem(),
                    productIdField.getText(),
                    Integer.parseInt(quantityField.getText())
            );

            RecordSaleResponse response = connection.recordSale(request);

            if (response.isSuccess()) {
                statusLabel.setForeground(new Color(0, 128, 0));
                statusLabel.setText(String.format("נרכש בהצלחה. סכום לתשלום: %.2f", response.getFinalAmount()));
                refreshInventory();
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (NumberFormatException e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("כמות לא תקינה");
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }
}
```

Save as `src/main/java/client/PurchasePanel.java`.

- [ ] **Step 2: Update `DashboardPanel`**

Replace the full contents of `src/main/java/client/DashboardPanel.java`:

```java
package client;

import model.Employee;
import model.Role;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    public DashboardPanel(Employee employee, Runnable onOpenAdmin, Runnable onOpenPurchase) {
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

        JButton purchaseButton = new JButton("רכישה");
        purchaseButton.addActionListener(e -> onOpenPurchase.run());
        c.gridy = 3;
        add(purchaseButton, c);

        if (employee.getRole() == Role.MANAGER) {
            JButton adminButton = new JButton("ניהול חשבונות");
            adminButton.addActionListener(e -> onOpenAdmin.run());
            c.gridy = 4;
            add(adminButton, c);
        }
    }
}
```

- [ ] **Step 3: Update `MainFrame`**

In `src/main/java/client/MainFrame.java`, add the constant `private static final String PURCHASE_CARD = "purchase";` alongside the other card constants, change the `showDashboard` call site, and add a `showPurchase` method:

```java
    private void showDashboard(Employee employee) {
        cards.add(new DashboardPanel(employee, () -> showAdmin(employee), this::showPurchase), DASHBOARD_CARD);
        cardLayout.show(cards, DASHBOARD_CARD);
    }

    private void showPurchase() {
        cards.add(new PurchasePanel(connection), PURCHASE_CARD);
        cardLayout.show(cards, PURCHASE_CARD);
    }
```

(Keep `showAdmin` as-is.)

- [ ] **Step 4: Manual sanity check (not part of the JUnit suite)**

Run `server.Main`, then `client.Main`. Log in as any seeded employee. Click "רכישה" — confirm the inventory table populates with the 3 seeded products and their real stock counts. Click a row (auto-fills the product id), enter a quantity and customer details, submit — confirm the success message shows the discounted amount and the table refreshes with reduced stock.

- [ ] **Step 5: Run the full test suite one final time**

Run: `mvn test`
Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/client/PurchasePanel.java src/main/java/client/DashboardPanel.java src/main/java/client/MainFrame.java
git commit -m "feat: add Purchase screen showing live inventory"
```

---

## Self-Review Notes

- **Spec coverage:** Closes the "purchase/sale interface that displays existing inventory" gap identified in the post-Stage-5 review. `PurchasePanel` shows real, live inventory (fetched over the network, not hardcoded) and refreshes after every purchase, demonstrating the per-branch inventory is genuinely shared and updated in real time.
- **Type consistency:** `GetInventoryResponse`/`InventoryItem` shapes match between `SaleService`, `ClientHandler`, `ServerConnection`, and `PurchasePanel`.
- **No new design pattern, no new dependency** — reuses the exact request/response and DTO-projection shapes established since Stage 3.
