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
