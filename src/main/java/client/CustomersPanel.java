package client;

import model.Customer;
import server.GetCustomersResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CustomersPanel extends JPanel {
    private final ServerConnection connection;

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new Object[]{"שם מלא", "ת\"ז", "טלפון", "סוג לקוח"}, 0);
    private final JLabel statusLabel = new JLabel(" ");

    public CustomersPanel(ServerConnection connection, Runnable onBack) {
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
            GetCustomersResponse response = connection.getCustomers();
            tableModel.setRowCount(0);
            for (Customer customer : response.getCustomers()) {
                tableModel.addRow(new Object[]{
                        customer.getFullName(),
                        customer.getIdNumber(),
                        customer.getPhone(),
                        customer.getCustomerType()
                });
            }
            statusLabel.setText(response.getCustomers().size() + " לקוחות ברשת");
        } catch (Exception e) {
            statusLabel.setText("שגיאה בטעינת הלקוחות: " + e.getMessage());
        }
    }
}
