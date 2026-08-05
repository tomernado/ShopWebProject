package client;

import model.Employee;
import model.Role;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    public DashboardPanel(Employee employee, Runnable onOpenAdmin, Runnable onOpenPurchase,
                           Runnable onOpenCustomers, Runnable onOpenEmployees,
                           Runnable onOpenReports, Runnable onOpenLogs) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;

        c.gridy = 0;
        add(new JLabel("שלום, " + employee.getFullName()), c);
        c.gridy = 1;
        add(new JLabel("תפקיד: " + employee.getRole()), c);
        c.gridy = 2;
        add(new JLabel("סניף: " + employee.getBranch().getName()), c);

        int row = 3;
        row = addButton(c, row, "רכישה", onOpenPurchase);
        row = addButton(c, row, "לקוחות", onOpenCustomers);
        row = addButton(c, row, "עובדים", onOpenEmployees);
        row = addButton(c, row, "דוחות", onOpenReports);

        if (employee.getRole() == Role.MANAGER) {
            row = addButton(c, row, "ניהול חשבונות", onOpenAdmin);
            addButton(c, row, "לוגים", onOpenLogs);
        }
    }

    private int addButton(GridBagConstraints c, int row, String label, Runnable onClick) {
        JButton button = new JButton(label);
        button.addActionListener(e -> onClick.run());
        c.gridy = row;
        add(button, c);
        return row + 1;
    }
}
