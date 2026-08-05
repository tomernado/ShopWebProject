# Stage 1 — Entities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Stage 1 entity classes (Employee, Branch, Product, Inventory, and the polymorphic Customer hierarchy) with JUnit tests, as a standalone Maven module — no server, no GUI, no files.

**Architecture:** Plain Java model classes under a single `model` package, each with a matching test class. The `Customer` hierarchy uses ordinary inheritance (`abstract class Customer` + 3 subclasses) with one abstract method (`calculateDiscount`) — no design pattern, no interfaces beyond what's needed.

**Tech Stack:** Java 17, Maven, JUnit 5 (Jupiter). Verified locally: Maven 3.9.15, Java 17 (Corretto).

## Global Constraints

- Academic course project — every line must be explainable in plain terms by the student. No design patterns, no frameworks, no libraries beyond JUnit.
- No over-engineering: no validation/edge-case handling beyond what a test requires. No getters/setters beyond what's used. No builder patterns, no Lombok.
- Package name is exactly `model` (not a reverse-domain package) per the approved spec at [2026-08-05-stage1-entities-design.md](../specs/2026-08-05-stage1-entities-design.md).
- Discount rule (from the spec, not user-configurable in this stage): New = 0%, Returning = 5%, VIP = 10%, applied via `Customer.calculateDiscount(double)`.
- Out of scope for this stage: authentication, real-time inventory broadcast, GUI, logging, JSON/Word export (all deferred to later stages per the overview spec).

---

### Task 1: Maven project scaffold + `Branch`

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/model/Branch.java`
- Test: `src/test/java/model/BranchTest.java`

**Interfaces:**
- Produces: `Branch(String branchId, String name, String address)`, `getBranchId()`, `getName()`, `getAddress()` — used by `Employee` and `Inventory` in later tasks.

- [ ] **Step 1: Create the Maven project structure and `pom.xml`**

Create `src/main/java/model/` and `src/test/java/model/` directories, then create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.shopchain</groupId>
    <artifactId>shop-chain-system</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write the failing test**

```java
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BranchTest {
    @Test
    void constructorStoresAllFields() {
        Branch branch = new Branch("B1", "Downtown", "1 Main St");

        assertEquals("B1", branch.getBranchId());
        assertEquals("Downtown", branch.getName());
        assertEquals("1 Main St", branch.getAddress());
    }
}
```

Save as `src/test/java/model/BranchTest.java`.

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn test -Dtest=BranchTest`
Expected: build failure — `cannot find symbol: class Branch`.

- [ ] **Step 4: Implement `Branch`**

```java
package model;

public class Branch {
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

Save as `src/main/java/model/Branch.java`.

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn test -Dtest=BranchTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/model/Branch.java src/test/java/model/BranchTest.java
git commit -m "feat: add Maven project scaffold and Branch entity"
```

---

### Task 2: `Product`

**Files:**
- Create: `src/main/java/model/Product.java`
- Test: `src/test/java/model/ProductTest.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `Product(String productId, String name, String category, double price)`, `getProductId()`, `getName()`, `getCategory()`, `getPrice()` — used by `Inventory` in Task 4.

- [ ] **Step 1: Write the failing test**

```java
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {
    @Test
    void constructorStoresAllFields() {
        Product product = new Product("P1", "Milk", "Dairy", 6.5);

        assertEquals("P1", product.getProductId());
        assertEquals("Milk", product.getName());
        assertEquals("Dairy", product.getCategory());
        assertEquals(6.5, product.getPrice());
    }

    @Test
    void negativePriceThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product("P2", "Bad", "Dairy", -1));
    }
}
```

Save as `src/test/java/model/ProductTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=ProductTest`
Expected: build failure — `cannot find symbol: class Product`.

- [ ] **Step 3: Implement `Product`**

```java
package model;

public class Product {
    private final String productId;
    private final String name;
    private final String category;
    private final double price;

    public Product(String productId, String name, String category, double price) {
        if (price < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }
}
```

Save as `src/main/java/model/Product.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=ProductTest`
Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/model/Product.java src/test/java/model/ProductTest.java
git commit -m "feat: add Product entity"
```

---

### Task 3: `Role` + `Employee`

**Files:**
- Create: `src/main/java/model/Role.java`
- Create: `src/main/java/model/Employee.java`
- Test: `src/test/java/model/EmployeeTest.java`

**Interfaces:**
- Consumes: `Branch` from Task 1 (`Branch(String, String, String)`).
- Produces: `enum Role { MANAGER, CASHIER, SELLER }`; `Employee(String idNumber, String fullName, String username, Role role, Branch branch)` with matching getters.

- [ ] **Step 1: Write the failing test**

```java
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeTest {
    @Test
    void constructorStoresAllFields() {
        Branch branch = new Branch("B1", "Downtown", "1 Main St");
        Employee employee = new Employee("123456789", "Dana Levi", "dana.l", Role.CASHIER, branch);

        assertEquals("123456789", employee.getIdNumber());
        assertEquals("Dana Levi", employee.getFullName());
        assertEquals("dana.l", employee.getUsername());
        assertEquals(Role.CASHIER, employee.getRole());
        assertEquals(branch, employee.getBranch());
    }
}
```

Save as `src/test/java/model/EmployeeTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=EmployeeTest`
Expected: build failure — `cannot find symbol: class Employee` (and `Role`).

- [ ] **Step 3: Implement `Role` and `Employee`**

```java
package model;

public enum Role {
    MANAGER,
    CASHIER,
    SELLER
}
```

Save as `src/main/java/model/Role.java`.

```java
package model;

public class Employee {
    private final String idNumber;
    private final String fullName;
    private final String username;
    private final Role role;
    private final Branch branch;

    public Employee(String idNumber, String fullName, String username, Role role, Branch branch) {
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.username = username;
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

    public Role getRole() {
        return role;
    }

    public Branch getBranch() {
        return branch;
    }
}
```

Save as `src/main/java/model/Employee.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=EmployeeTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/model/Role.java src/main/java/model/Employee.java src/test/java/model/EmployeeTest.java
git commit -m "feat: add Role enum and Employee entity"
```

---

### Task 4: `Inventory`

**Files:**
- Create: `src/main/java/model/Inventory.java`
- Test: `src/test/java/model/InventoryTest.java`

**Interfaces:**
- Consumes: `Branch` from Task 1, `Product` from Task 2.
- Produces: `Inventory(Branch branch)`, `getBranch()`, `getQuantity(Product)`, `addStock(Product, int)`, `reduceStock(Product, int)` — not consumed by other Stage 1 tasks, but this is the signature Stage 2 (Server) will call.

- [ ] **Step 1: Write the failing test**

```java
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryTest {
    private final Branch branch = new Branch("B1", "Downtown", "1 Main St");
    private final Product milk = new Product("P1", "Milk", "Dairy", 6.5);

    @Test
    void newInventoryHasZeroQuantityForUnknownProduct() {
        Inventory inventory = new Inventory(branch);
        assertEquals(0, inventory.getQuantity(milk));
    }

    @Test
    void addStockIncreasesQuantity() {
        Inventory inventory = new Inventory(branch);
        inventory.addStock(milk, 10);
        assertEquals(10, inventory.getQuantity(milk));
    }

    @Test
    void reduceStockDecreasesQuantity() {
        Inventory inventory = new Inventory(branch);
        inventory.addStock(milk, 10);
        inventory.reduceStock(milk, 4);
        assertEquals(6, inventory.getQuantity(milk));
    }

    @Test
    void reduceStockBelowZeroThrows() {
        Inventory inventory = new Inventory(branch);
        inventory.addStock(milk, 3);
        assertThrows(IllegalStateException.class, () -> inventory.reduceStock(milk, 4));
    }
}
```

Save as `src/test/java/model/InventoryTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=InventoryTest`
Expected: build failure — `cannot find symbol: class Inventory`.

- [ ] **Step 3: Implement `Inventory`**

```java
package model;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private final Branch branch;
    private final Map<Product, Integer> stock = new HashMap<>();

    public Inventory(Branch branch) {
        this.branch = branch;
    }

    public Branch getBranch() {
        return branch;
    }

    public int getQuantity(Product product) {
        return stock.getOrDefault(product, 0);
    }

    public void addStock(Product product, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        stock.merge(product, amount, Integer::sum);
    }

    public void reduceStock(Product product, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        int current = getQuantity(product);
        if (amount > current) {
            throw new IllegalStateException("not enough stock for " + product.getName());
        }
        stock.put(product, current - amount);
    }
}
```

Save as `src/main/java/model/Inventory.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=InventoryTest`
Expected: `BUILD SUCCESS`, 4 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/model/Inventory.java src/test/java/model/InventoryTest.java
git commit -m "feat: add Inventory entity with stock add/reduce"
```

---

### Task 5: `CustomerType` + `Customer` (abstract) + `NewCustomer`

**Files:**
- Create: `src/main/java/model/CustomerType.java`
- Create: `src/main/java/model/Customer.java`
- Create: `src/main/java/model/NewCustomer.java`
- Test: `src/test/java/model/CustomerDiscountTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `abstract class Customer` with constructor `Customer(String fullName, String idNumber, String phone, CustomerType customerType)`, abstract method `double calculateDiscount(double totalAmount)`, concrete method `double purchase(double totalAmount)`. `NewCustomer(String fullName, String idNumber, String phone)`. Task 6 extends this same file set with `ReturningCustomer` and `VipCustomer`.

- [ ] **Step 1: Write the failing test**

```java
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerDiscountTest {

    @Test
    void newCustomerGetsNoDiscount() {
        Customer customer = new NewCustomer("Yossi Cohen", "111111111", "0501111111");
        assertEquals(100.0, customer.purchase(100.0), 0.0001);
    }

    @Test
    void newCustomerZeroAmountStaysZero() {
        Customer customer = new NewCustomer("Yossi Cohen", "111111111", "0501111111");
        assertEquals(0.0, customer.purchase(0.0), 0.0001);
    }
}
```

Save as `src/test/java/model/CustomerDiscountTest.java`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=CustomerDiscountTest`
Expected: build failure — `cannot find symbol: class Customer` (and `NewCustomer`).

- [ ] **Step 3: Implement `CustomerType`, `Customer`, `NewCustomer`**

```java
package model;

public enum CustomerType {
    NEW,
    RETURNING,
    VIP
}
```

Save as `src/main/java/model/CustomerType.java`.

```java
package model;

public abstract class Customer {
    private final String fullName;
    private final String idNumber;
    private final String phone;
    private final CustomerType customerType;

    protected Customer(String fullName, String idNumber, String phone, CustomerType customerType) {
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.phone = phone;
        this.customerType = customerType;
    }

    public String getFullName() {
        return fullName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getPhone() {
        return phone;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public abstract double calculateDiscount(double totalAmount);

    public double purchase(double totalAmount) {
        return totalAmount - calculateDiscount(totalAmount);
    }
}
```

Save as `src/main/java/model/Customer.java`.

```java
package model;

public class NewCustomer extends Customer {
    public NewCustomer(String fullName, String idNumber, String phone) {
        super(fullName, idNumber, phone, CustomerType.NEW);
    }

    @Override
    public double calculateDiscount(double totalAmount) {
        return 0;
    }
}
```

Save as `src/main/java/model/NewCustomer.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=CustomerDiscountTest`
Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/model/CustomerType.java src/main/java/model/Customer.java src/main/java/model/NewCustomer.java src/test/java/model/CustomerDiscountTest.java
git commit -m "feat: add Customer abstract class and NewCustomer"
```

---

### Task 6: `ReturningCustomer` + `VipCustomer` (+ polymorphism tests)

**Files:**
- Create: `src/main/java/model/ReturningCustomer.java`
- Create: `src/main/java/model/VipCustomer.java`
- Modify: `src/test/java/model/CustomerDiscountTest.java` (replace with the full version below, adding Returning/VIP/polymorphism tests to the two from Task 5)

**Interfaces:**
- Consumes: `Customer` and `CustomerType` from Task 5.
- Produces: `ReturningCustomer(String, String, String)`, `VipCustomer(String, String, String)` — completes the hierarchy; no later Stage 1 task depends on these.

- [ ] **Step 1: Extend the test file with failing tests**

Replace the full contents of `src/test/java/model/CustomerDiscountTest.java` with:

```java
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerDiscountTest {

    @Test
    void newCustomerGetsNoDiscount() {
        Customer customer = new NewCustomer("Yossi Cohen", "111111111", "0501111111");
        assertEquals(100.0, customer.purchase(100.0), 0.0001);
    }

    @Test
    void newCustomerZeroAmountStaysZero() {
        Customer customer = new NewCustomer("Yossi Cohen", "111111111", "0501111111");
        assertEquals(0.0, customer.purchase(0.0), 0.0001);
    }

    @Test
    void returningCustomerGetsFivePercentDiscount() {
        Customer customer = new ReturningCustomer("Noa Levi", "222222222", "0502222222");
        assertEquals(95.0, customer.purchase(100.0), 0.0001);
    }

    @Test
    void vipCustomerGetsTenPercentDiscount() {
        Customer customer = new VipCustomer("Roi Biton", "333333333", "0503333333");
        assertEquals(90.0, customer.purchase(100.0), 0.0001);
    }

    @Test
    void purchaseIsPolymorphicAcrossCustomerTypes() {
        Customer[] customers = {
                new NewCustomer("A", "1", "050"),
                new ReturningCustomer("B", "2", "050"),
                new VipCustomer("C", "3", "050")
        };
        double[] expected = {100.0, 95.0, 90.0};

        for (int i = 0; i < customers.length; i++) {
            assertEquals(expected[i], customers[i].purchase(100.0), 0.0001);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -Dtest=CustomerDiscountTest`
Expected: build failure — `cannot find symbol: class ReturningCustomer` (and `VipCustomer`).

- [ ] **Step 3: Implement `ReturningCustomer` and `VipCustomer`**

```java
package model;

public class ReturningCustomer extends Customer {
    private static final double DISCOUNT_RATE = 0.05;

    public ReturningCustomer(String fullName, String idNumber, String phone) {
        super(fullName, idNumber, phone, CustomerType.RETURNING);
    }

    @Override
    public double calculateDiscount(double totalAmount) {
        return totalAmount * DISCOUNT_RATE;
    }
}
```

Save as `src/main/java/model/ReturningCustomer.java`.

```java
package model;

public class VipCustomer extends Customer {
    private static final double DISCOUNT_RATE = 0.10;

    public VipCustomer(String fullName, String idNumber, String phone) {
        super(fullName, idNumber, phone, CustomerType.VIP);
    }

    @Override
    public double calculateDiscount(double totalAmount) {
        return totalAmount * DISCOUNT_RATE;
    }
}
```

Save as `src/main/java/model/VipCustomer.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -Dtest=CustomerDiscountTest`
Expected: `BUILD SUCCESS`, 5 tests run, 0 failures.

- [ ] **Step 5: Run the full test suite**

Run: `mvn test`
Expected: `BUILD SUCCESS`, all tests across `BranchTest`, `ProductTest`, `EmployeeTest`, `InventoryTest`, `CustomerDiscountTest` pass (13 tests total).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/model/ReturningCustomer.java src/main/java/model/VipCustomer.java src/test/java/model/CustomerDiscountTest.java
git commit -m "feat: add ReturningCustomer and VipCustomer, complete discount hierarchy"
```

---

## Self-Review Notes

- **Spec coverage:** All 4 fields on `Customer` from the spec are present; the discount rule (0%/5%/10%) matches section 3.6 of the design; `Employee` fields match section 3.2; `Inventory` uses `Map<Product, Integer>` as specified in 3.5.
- **Type consistency:** `Product` is used as a `Map` key in `Inventory` — relies on default `Object` identity equality, which is fine here since Stage 1 never needs to look up a `Product` by a separately-constructed equal instance (each test reuses the same `Product` reference). No `equals()`/`hashCode()` override needed at this stage; call out if a later stage needs value-based lookup.
- **No design patterns introduced** beyond the plain inheritance the spec explicitly requires.
