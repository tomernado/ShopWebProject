package server;

import model.Product;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogTest {
    private final ProductCatalog catalog = new ProductCatalog(new EmployeeDirectory());

    @Test
    void findsSeededProductById() {
        Product product = catalog.findProductById("P1");

        assertNotNull(product);
        assertEquals("Milk", product.getName());
    }

    @Test
    void unknownProductIdReturnsNull() {
        assertNull(catalog.findProductById("nope"));
    }

    @Test
    void eachSeededBranchHasStockedInventory() {
        var inventory = catalog.getInventoryForBranch("B1");
        Product milk = catalog.findProductById("P1");

        assertNotNull(inventory);
        assertEquals(50, inventory.getQuantity(milk));
    }

    @Test
    void unknownBranchHasNoInventory() {
        assertNull(catalog.getInventoryForBranch("nope"));
    }

    @Test
    void getAllProductsReturnsAllThreeSeededProducts() {
        assertEquals(3, catalog.getAllProducts().size());
    }
}
