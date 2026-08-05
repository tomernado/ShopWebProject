package client;

import model.Branch;
import model.Employee;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    // Hardcoded to match server.EmployeeDirectory's seed data — see the
    // Stage 3 design spec for why the client doesn't fetch this over the wire.
    private static final List<Branch> BRANCHES = List.of(
            new Branch("B1", "Downtown", "1 Main St"),
            new Branch("B2", "Uptown", "2 High St")
    );

    private static final String LOGIN_CARD = "login";
    private static final String DASHBOARD_CARD = "dashboard";
    private static final String ADMIN_CARD = "admin";
    private static final String PURCHASE_CARD = "purchase";
    private static final String CUSTOMERS_CARD = "customers";
    private static final String EMPLOYEES_CARD = "employees";
    private static final String REPORTS_CARD = "reports";
    private static final String LOGS_CARD = "logs";

    private final ServerConnection connection;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private Employee loggedInEmployee;

    public MainFrame(ServerConnection connection) {
        super("מערכת ניהול רשת חנויות");
        this.connection = connection;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        cards.add(new LoginPanel(connection, this::showDashboard), LOGIN_CARD);
        add(cards);

        cardLayout.show(cards, LOGIN_CARD);
    }

    private void showDashboard(Employee employee) {
        this.loggedInEmployee = employee;
        cards.add(new DashboardPanel(
                employee,
                () -> showAdmin(employee),
                this::showPurchase,
                this::showCustomers,
                this::showEmployees,
                this::showReports,
                this::showLogs
        ), DASHBOARD_CARD);
        cardLayout.show(cards, DASHBOARD_CARD);
    }

    private void showDashboardAgain() {
        showDashboard(loggedInEmployee);
    }

    private void showAdmin(Employee employee) {
        cards.add(new AdminPanel(connection, BRANCHES, this::showDashboardAgain), ADMIN_CARD);
        cardLayout.show(cards, ADMIN_CARD);
    }

    private void showPurchase() {
        cards.add(new PurchasePanel(connection, this::showDashboardAgain), PURCHASE_CARD);
        cardLayout.show(cards, PURCHASE_CARD);
    }

    private void showCustomers() {
        cards.add(new CustomersPanel(connection, this::showDashboardAgain), CUSTOMERS_CARD);
        cardLayout.show(cards, CUSTOMERS_CARD);
    }

    private void showEmployees() {
        cards.add(new EmployeesPanel(connection, this::showDashboardAgain), EMPLOYEES_CARD);
        cardLayout.show(cards, EMPLOYEES_CARD);
    }

    private void showReports() {
        cards.add(new ReportsPanel(connection, this::showDashboardAgain), REPORTS_CARD);
        cardLayout.show(cards, REPORTS_CARD);
    }

    private void showLogs() {
        cards.add(new LogsPanel(connection, this::showDashboardAgain), LOGS_CARD);
        cardLayout.show(cards, LOGS_CARD);
    }
}
