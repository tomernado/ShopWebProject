package server;

import model.Employee;

import java.io.Serializable;
import java.util.List;

public class GetEmployeesResponse implements Serializable {
    private final List<Employee> employees;

    public GetEmployeesResponse(List<Employee> employees) {
        this.employees = employees;
    }

    public List<Employee> getEmployees() {
        return employees;
    }
}
