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
