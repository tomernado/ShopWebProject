package reports;

import java.io.Serializable;

public class SalesReportLine implements Serializable {
    private final String key;
    private final int totalQuantity;
    private final double totalRevenue;

    public SalesReportLine(String key, int totalQuantity, double totalRevenue) {
        this.key = key;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }

    public String getKey() {
        return key;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }
}
