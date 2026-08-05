package reports;

import java.io.Serializable;
import java.util.List;

public class SalesReport implements Serializable {
    private final String groupedBy;
    private final List<SalesReportLine> lines;

    public SalesReport(String groupedBy, List<SalesReportLine> lines) {
        this.groupedBy = groupedBy;
        this.lines = lines;
    }

    public String getGroupedBy() {
        return groupedBy;
    }

    public List<SalesReportLine> getLines() {
        return lines;
    }
}
