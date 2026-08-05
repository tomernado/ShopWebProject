package server;

import java.io.Serializable;

public class RecordSaleResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;
    private final double finalAmount;

    private RecordSaleResponse(boolean success, String errorMessage, double finalAmount) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.finalAmount = finalAmount;
    }

    public static RecordSaleResponse success(double finalAmount) {
        return new RecordSaleResponse(true, null, finalAmount);
    }

    public static RecordSaleResponse failure(String errorMessage) {
        return new RecordSaleResponse(false, errorMessage, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinalAmount() {
        return finalAmount;
    }
}
