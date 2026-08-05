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
