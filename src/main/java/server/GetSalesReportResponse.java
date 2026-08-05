package server;

import reports.SalesReport;

import java.io.Serializable;

public class GetSalesReportResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;
    private final SalesReport report;

    private GetSalesReportResponse(boolean success, String errorMessage, SalesReport report) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.report = report;
    }

    public static GetSalesReportResponse success(SalesReport report) {
        return new GetSalesReportResponse(true, null, report);
    }

    public static GetSalesReportResponse failure(String errorMessage) {
        return new GetSalesReportResponse(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public SalesReport getReport() {
        return report;
    }
}
