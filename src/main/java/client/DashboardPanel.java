package client;

import model.Employee;
import model.Role;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    public DashboardPanel(Employee employee, Runnable onOpenAdmin) {
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

        if (employee.getRole() == Role.MANAGER) {
            JButton adminButton = new JButton("ניהול חשבונות");
            adminButton.addActionListener(e -> onOpenAdmin.run());
            c.gridy = 3;
            add(adminButton, c);
        }
    }
}
