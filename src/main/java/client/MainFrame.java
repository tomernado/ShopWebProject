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

    private final ServerConnection connection;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public MainFrame(ServerConnection connection) {
        super("מערכת ניהול רשת חנויות");
        this.connection = connection;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        cards.add(new LoginPanel(connection, this::showDashboard), LOGIN_CARD);
        add(cards);

        cardLayout.show(cards, LOGIN_CARD);
    }

    private void showDashboard(Employee employee) {
        cards.add(new DashboardPanel(employee, () -> showAdmin(employee)), DASHBOARD_CARD);
        cardLayout.show(cards, DASHBOARD_CARD);
    }

    private void showAdmin(Employee employee) {
        cards.add(new AdminPanel(connection, BRANCHES), ADMIN_CARD);
        cardLayout.show(cards, ADMIN_CARD);
    }
}
