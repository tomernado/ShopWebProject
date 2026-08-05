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

    @Test
    void returningCustomerGetsFivePercentDiscount() {
        Customer customer = new ReturningCustomer("Noa Levi", "222222222", "0502222222");
        assertEquals(95.0, customer.purchase(100.0), 0.0001);
    }

    @Test
    void vipCustomerGetsTenPercentDiscount() {
        Customer customer = new VipCustomer("Roi Biton", "333333333", "0503333333");
        assertEquals(90.0, customer.purchase(100.0), 0.0001);
    }

    @Test
    void purchaseIsPolymorphicAcrossCustomerTypes() {
        Customer[] customers = {
                new NewCustomer("A", "1", "050"),
                new ReturningCustomer("B", "2", "050"),
                new VipCustomer("C", "3", "050")
        };
        double[] expected = {100.0, 95.0, 90.0};

        for (int i = 0; i < customers.length; i++) {
            assertEquals(expected[i], customers[i].purchase(100.0), 0.0001);
        }
    }
}
