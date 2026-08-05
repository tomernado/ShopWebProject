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
