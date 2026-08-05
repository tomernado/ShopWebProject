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
