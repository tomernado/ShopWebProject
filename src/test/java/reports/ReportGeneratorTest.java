package reports;

import model.CustomerType;
import org.junit.jupiter.api.Test;
import server.SaleRecord;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {
    private final ReportGenerator generator = new ReportGenerator();

    private SaleRecord sale(String branchId, String productName, String category, int quantity, double amount) {
        return new SaleRecord(branchId, "yossi.c", "Customer A", CustomerType.NEW,
                "P1", productName, category, quantity, amount, LocalDateTime.now());
    }

    @Test
    void byBranchAggregatesQuantityAndRevenuePerBranch() {
        List<SaleRecord> sales = List.of(
                sale("B1", "Milk", "Dairy", 2, 13.0),
                sale("B1", "Bread", "Bakery", 1, 8.0),
                sale("B2", "Milk", "Dairy", 3, 19.5)
        );

        SalesReport report = generator.byBranch(sales);

        assertEquals("branch", report.getGroupedBy());
        assertEquals(2, report.getLines().size());

        SalesReportLine b1 = findLine(report, "B1");
        assertEquals(3, b1.getTotalQuantity());
        assertEquals(21.0, b1.getTotalRevenue(), 0.0001);
    }

    @Test
    void byProductAggregatesAcrossBranches() {
        List<SaleRecord> sales = List.of(
                sale("B1", "Milk", "Dairy", 2, 13.0),
                sale("B2", "Milk", "Dairy", 3, 19.5)
        );

        SalesReport report = generator.byProduct(sales);

        SalesReportLine milk = findLine(report, "Milk");
        assertEquals(5, milk.getTotalQuantity());
        assertEquals(32.5, milk.getTotalRevenue(), 0.0001);
    }

    @Test
    void byCategoryGroupsDifferentProductsTogether() {
        List<SaleRecord> sales = List.of(
                sale("B1", "Milk", "Dairy", 2, 13.0),
                sale("B1", "Cheese", "Dairy", 1, 22.0)
        );

        SalesReport report = generator.byCategory(sales);

        assertEquals(1, report.getLines().size());
        SalesReportLine dairy = findLine(report, "Dairy");
        assertEquals(3, dairy.getTotalQuantity());
        assertEquals(35.0, dairy.getTotalRevenue(), 0.0001);
    }

    @Test
    void emptySalesListProducesAnEmptyReport() {
        SalesReport report = generator.byBranch(List.of());

        assertTrue(report.getLines().isEmpty());
    }

    private SalesReportLine findLine(SalesReport report, String key) {
        return report.getLines().stream()
                .filter(line -> line.getKey().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
