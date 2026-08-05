package reports;

import server.SaleRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReportGenerator {

    public SalesReport byBranch(List<SaleRecord> sales) {
        return group("branch", sales, SaleRecord::getBranchId);
    }

    public SalesReport byProduct(List<SaleRecord> sales) {
        return group("product", sales, SaleRecord::getProductName);
    }

    public SalesReport byCategory(List<SaleRecord> sales) {
        return group("category", sales, SaleRecord::getCategory);
    }

    private SalesReport group(String groupedBy, List<SaleRecord> sales, Function<SaleRecord, String> keyExtractor) {
        Map<String, Integer> quantityByKey = new LinkedHashMap<>();
        Map<String, Double> revenueByKey = new LinkedHashMap<>();

        for (SaleRecord sale : sales) {
            String key = keyExtractor.apply(sale);
            quantityByKey.merge(key, sale.getQuantity(), Integer::sum);
            revenueByKey.merge(key, sale.getFinalAmount(), Double::sum);
        }

        List<SalesReportLine> lines = new ArrayList<>();
        for (String key : quantityByKey.keySet()) {
            lines.add(new SalesReportLine(key, quantityByKey.get(key), revenueByKey.get(key)));
        }

        return new SalesReport(groupedBy, lines);
    }
}
