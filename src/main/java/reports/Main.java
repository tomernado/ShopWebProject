package reports;

import model.CustomerType;
import model.Employee;
import server.EmployeeDirectory;
import server.ProductCatalog;
import server.RecordSaleRequest;
import server.SaleService;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        ProductCatalog productCatalog = new ProductCatalog(employeeDirectory);
        SaleService saleService = new SaleService(productCatalog);

        Employee dana = employeeDirectory.findByUsername("dana.l");
        Employee yossi = employeeDirectory.findByUsername("yossi.c");
        Employee noa = employeeDirectory.findByUsername("noa.b");

        saleService.recordSale(yossi, new RecordSaleRequest("Alice", "1", "050-0000001", CustomerType.NEW, "P1", 3));
        saleService.recordSale(yossi, new RecordSaleRequest("Bob", "2", "050-0000002", CustomerType.RETURNING, "P2", 2));
        saleService.recordSale(noa, new RecordSaleRequest("Carol", "3", "050-0000003", CustomerType.VIP, "P1", 4));
        saleService.recordSale(dana, new RecordSaleRequest("Dave", "4", "050-0000004", CustomerType.NEW, "P3", 1));

        ReportGenerator generator = new ReportGenerator();
        JsonReportExporter jsonExporter = new JsonReportExporter();
        WordReportExporter wordExporter = new WordReportExporter();

        exportBoth(generator.byBranch(saleService.getSalesLedger()), "by-branch", jsonExporter, wordExporter);
        exportBoth(generator.byProduct(saleService.getSalesLedger()), "by-product", jsonExporter, wordExporter);
        exportBoth(generator.byCategory(saleService.getSalesLedger()), "by-category", jsonExporter, wordExporter);

        System.out.println("Reports written to the reports/ directory.");
    }

    private static void exportBoth(SalesReport report, String baseName,
                                    JsonReportExporter jsonExporter, WordReportExporter wordExporter) throws Exception {
        jsonExporter.export(report, Path.of("reports", baseName + ".json"));
        wordExporter.export(report, Path.of("reports", baseName + ".docx"));
    }
}
