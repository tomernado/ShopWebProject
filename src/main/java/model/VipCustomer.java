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
