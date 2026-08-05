package server;

import model.Branch;
import model.CustomerType;
import model.Employee;
import model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaleServiceTest {
    private final EmployeeDirectory employeeDirectory = new EmployeeDirectory();
    private final ProductCatalog productCatalog = new ProductCatalog(employeeDirectory);
    private final SaleService saleService = new SaleService(productCatalog);
    private final Branch branch = employeeDirectory.findBranchById("B1");
    private final Employee cashier = new Employee("1", "Yossi Cohen", "yossi.c", "pw", Role.CASHIER, branch);

    @Test
    void newCustomerPaysFullPriceAndStockIsReduced() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "P1", 2);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertTrue(response.isSuccess());
        assertEquals(13.0, response.getFinalAmount(), 0.0001); // 2 * 6.5, no discount
        assertEquals(48, productCatalog.getInventoryForBranch("B1").getQuantity(productCatalog.findProductById("P1")));
    }

    @Test
    void vipCustomerGetsTenPercentDiscount() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer B", "2", "050", CustomerType.VIP, "P1", 2);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertEquals(11.7, response.getFinalAmount(), 0.0001); // 13 - 10%
    }

    @Test
    void unknownProductFails() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "no-such-product", 1);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void insufficientStockFails() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "P1", 1000);

        RecordSaleResponse response = saleService.recordSale(cashier, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void successfulSaleIsAddedToTheLedger() {
        RecordSaleRequest request = new RecordSaleRequest(
                "Customer A", "1", "050", CustomerType.NEW, "P1", 1);

        saleService.recordSale(cashier, request);

        assertEquals(1, saleService.getSalesLedger().size());
        assertEquals("Customer A", saleService.getSalesLedger().get(0).getCustomerFullName());
    }

    @Test
    void inventorySnapshotReflectsCurrentStockForEmployeesBranch() {
        var snapshot = saleService.getInventorySnapshot(cashier);

        assertEquals(3, snapshot.size());
        var milk = snapshot.stream().filter(item -> item.getProductId().equals("P1")).findFirst().orElseThrow();
        assertEquals(50, milk.getAvailableQuantity());
    }

    @Test
    void inventorySnapshotReflectsStockAfterASale() {
        saleService.recordSale(cashier, new RecordSaleRequest("A", "1", "050", CustomerType.NEW, "P1", 5));

        var snapshot = saleService.getInventorySnapshot(cashier);
        var milk = snapshot.stream().filter(item -> item.getProductId().equals("P1")).findFirst().orElseThrow();

        assertEquals(45, milk.getAvailableQuantity());
    }
}
