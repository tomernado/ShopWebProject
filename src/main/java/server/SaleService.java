package server;

import logging.SystemLogger;
import model.Customer;
import model.Employee;
import model.Inventory;
import model.NewCustomer;
import model.Product;
import model.ReturningCustomer;
import model.VipCustomer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaleService {
    private final ProductCatalog productCatalog;
    private final List<SaleRecord> salesLedger = new ArrayList<>();

    public SaleService(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    public synchronized RecordSaleResponse recordSale(Employee employee, RecordSaleRequest request) {
        if (request.getQuantity() <= 0) {
            return RecordSaleResponse.failure("Quantity must be positive");
        }

        Product product = productCatalog.findProductById(request.getProductId());
        if (product == null) {
            return RecordSaleResponse.failure("Unknown product");
        }

        Inventory inventory = productCatalog.getInventoryForBranch(employee.getBranch().getBranchId());
        if (inventory.getQuantity(product) < request.getQuantity()) {
            return RecordSaleResponse.failure("Not enough stock");
        }

        Customer customer = buildCustomer(request);
        double totalBeforeDiscount = product.getPrice() * request.getQuantity();
        double finalAmount = customer.purchase(totalBeforeDiscount);

        inventory.reduceStock(product, request.getQuantity());

        SaleRecord record = new SaleRecord(
                employee.getBranch().getBranchId(),
                employee.getUsername(),
                request.getCustomerFullName(),
                request.getCustomerType(),
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                request.getQuantity(),
                finalAmount,
                LocalDateTime.now()
        );
        salesLedger.add(record);

        SystemLogger.getInstance().log("SALE", String.format(
                "%s sold %d x %s at branch %s for %.2f (customer: %s, %s)",
                employee.getUsername(), request.getQuantity(), product.getName(),
                employee.getBranch().getBranchId(), finalAmount,
                request.getCustomerFullName(), request.getCustomerType()));

        return RecordSaleResponse.success(finalAmount);
    }

    public synchronized List<InventoryItem> getInventorySnapshot(Employee employee) {
        Inventory inventory = productCatalog.getInventoryForBranch(employee.getBranch().getBranchId());
        List<InventoryItem> items = new ArrayList<>();
        for (Product product : productCatalog.getAllProducts()) {
            int quantity = inventory != null ? inventory.getQuantity(product) : 0;
            items.add(new InventoryItem(product.getProductId(), product.getName(), product.getCategory(),
                    product.getPrice(), quantity));
        }
        return items;
    }

    private Customer buildCustomer(RecordSaleRequest request) {
        String name = request.getCustomerFullName();
        String id = request.getCustomerIdNumber();
        String phone = request.getCustomerPhone();
        return switch (request.getCustomerType()) {
            case NEW -> new NewCustomer(name, id, phone);
            case RETURNING -> new ReturningCustomer(name, id, phone);
            case VIP -> new VipCustomer(name, id, phone);
        };
    }

    public synchronized List<SaleRecord> getSalesLedger() {
        return Collections.unmodifiableList(new ArrayList<>(salesLedger));
    }
}
