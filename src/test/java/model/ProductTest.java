package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {
    @Test
    void constructorStoresAllFields() {
        Product product = new Product("P1", "Milk", "Dairy", 6.5);

        assertEquals("P1", product.getProductId());
        assertEquals("Milk", product.getName());
        assertEquals("Dairy", product.getCategory());
        assertEquals(6.5, product.getPrice());
    }

    @Test
    void negativePriceThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product("P2", "Bad", "Dairy", -1));
    }
}
