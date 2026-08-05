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
