package client;

import model.Employee;
import server.GetEmployeesResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class EmployeesPanel extends JPanel {
    private final ServerConnection connection;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"שם מלא", "ת\"ז", "מס' עובד", "טלפון", "מס' חשבון", "תפקיד", "סניף"}, 0);
    private final JLabel statusLabel = new JLabel(" ");

    public EmployeesPanel(ServerConnection connection, Runnable onBack) {
        this.connection = connection;

        setLayout(new BorderLayout(10, 10));
        JButton backButton = new JButton("חזרה");
        backButton.addActionListener(e -> onBack.run());
        add(backButton, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);

        JButton refreshButton = new JButton("רענון");
        refreshButton.addActionListener(e -> refresh());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(refreshButton, BorderLayout.WEST);
        bottom.add(statusLabel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    private void refresh() {
        try {
            GetEmployeesResponse response = connection.getEmployees();
            tableModel.setRowCount(0);
            for (Employee employee : response.getEmployees()) {
                tableModel.addRow(new Object[]{
                        employee.getFullName(),
                        employee.getIdNumber(),
                        employee.getEmployeeNumber(),
                        employee.getPhone(),
                        employee.getAccountNumber(),
                        employee.getRole(),
                        employee.getBranch().getName()
                });
            }
            statusLabel.setText(response.getEmployees().size() + " עובדים ברשת");
        } catch (Exception e) {
            statusLabel.setText("שגיאה בטעינת העובדים: " + e.getMessage());
        }
    }
}
