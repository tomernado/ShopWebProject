package server;

import model.Customer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerDirectory {
    private final Map<String, Customer> customersById = new HashMap<>();

    public synchronized void registerOrUpdate(Customer customer) {
        customersById.put(customer.getIdNumber(), customer);
    }

    public synchronized Customer findById(String idNumber) {
        return customersById.get(idNumber);
    }

    public synchronized List<Customer> getAllCustomers() {
        return new ArrayList<>(customersById.values());
    }
}
