package server;

import java.io.Serializable;
import java.util.List;

public class GetInventoryResponse implements Serializable {
    private final List<InventoryItem> items;

    public GetInventoryResponse(List<InventoryItem> items) {
        this.items = items;
    }

    public List<InventoryItem> getItems() {
        return items;
    }
}
