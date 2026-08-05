package server;

import model.Customer;

import java.io.Serializable;
import java.util.List;

public class GetCustomersResponse implements Serializable {
    private final List<Customer> customers;

    public GetCustomersResponse(List<Customer> customers) {
        this.customers = customers;
    }

    public List<Customer> getCustomers() {
        return customers;
    }
}
