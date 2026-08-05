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
