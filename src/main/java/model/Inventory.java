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
