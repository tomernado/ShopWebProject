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
