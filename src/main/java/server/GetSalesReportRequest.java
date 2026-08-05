package server;

import java.io.Serializable;

public class GetSalesReportRequest implements Serializable {
    private final String groupBy;
    private final boolean todayOnly;

    public GetSalesReportRequest(String groupBy, boolean todayOnly) {
        this.groupBy = groupBy;
        this.todayOnly = todayOnly;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public boolean isTodayOnly() {
        return todayOnly;
    }
}
