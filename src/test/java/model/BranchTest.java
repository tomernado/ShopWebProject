package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BranchTest {
    @Test
    void constructorStoresAllFields() {
        Branch branch = new Branch("B1", "Downtown", "1 Main St");

        assertEquals("B1", branch.getBranchId());
        assertEquals("Downtown", branch.getName());
        assertEquals("1 Main St", branch.getAddress());
    }
}
