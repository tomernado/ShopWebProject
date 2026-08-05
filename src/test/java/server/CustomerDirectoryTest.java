package server;

import model.Customer;
import model.NewCustomer;
import model.ReturningCustomer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerDirectoryTest {
    private final CustomerDirectory directory = new CustomerDirectory();

    @Test
    void unknownCustomerReturnsNull() {
        assertNull(directory.findById("no-such-id"));
    }

    @Test
    void registeredCustomerIsFindableById() {
        Customer customer = new NewCustomer("Alice", "1", "050-0000001");

        directory.registerOrUpdate(customer);

        assertEquals(customer, directory.findById("1"));
    }

    @Test
    void reRegisteringTheSameIdUpdatesTheRecord() {
        directory.registerOrUpdate(new NewCustomer("Alice", "1", "050-0000001"));
        Customer updated = new ReturningCustomer("Alice", "1", "050-0000001");

        directory.registerOrUpdate(updated);

        assertEquals(1, directory.getAllCustomers().size());
        assertEquals(updated, directory.findById("1"));
    }

    @Test
    void getAllCustomersReturnsEveryRegisteredCustomer() {
        directory.registerOrUpdate(new NewCustomer("Alice", "1", "050-0000001"));
        directory.registerOrUpdate(new NewCustomer("Bob", "2", "050-0000002"));

        assertEquals(2, directory.getAllCustomers().size());
    }
}
