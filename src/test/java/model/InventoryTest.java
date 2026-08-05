package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {
    private final Branch branch = new Branch("B1", "Downtown", "1 Main St");
    private final Product milk = new Product("P1", "Milk", "Dairy", 6.5);

    @Test
    void newInventoryHasZeroQuantityForUnknownProduct() {
        Inventory inventory = new Inventory(branch);
        assertEquals(0, inventory.getQuantity(milk));
    }

    @Test
    void addStockIncreasesQuantity() {
        Inventory inventory = new Inventory(branch);
        inventory.addStock(milk, 10);
        assertEquals(10, inventory.getQuantity(milk));
    }

    @Test
    void reduceStockDecreasesQuantity() {
        Inventory inventory = new Inventory(branch);
        inventory.addStock(milk, 10);
        inventory.reduceStock(milk, 4);
        assertEquals(6, inventory.getQuantity(milk));
    }

    @Test
    void reduceStockBelowZeroThrows() {
        Inventory inventory = new Inventory(branch);
        inventory.addStock(milk, 3);
        assertThrows(IllegalStateException.class, () -> inventory.reduceStock(milk, 4));
    }
}
