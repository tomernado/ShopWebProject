package client;

import model.CustomerType;
import server.GetInventoryResponse;
import server.InventoryItem;
import server.RecordSaleRequest;
import server.RecordSaleResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PurchasePanel extends JPanel {
    private final ServerConnection connection;

    private final DefaultTableModel inventoryTableModel =
            new DefaultTableModel(new Object[]{"מוצר", "קטגוריה", "מחיר", "במלאי"}, 0);
    private final JTable inventoryTable = new JTable(inventoryTableModel);

    private final JTextField productIdField = new JTextField(10);
    private final JTextField quantityField = new JTextField(5);
    private final JTextField customerNameField = new JTextField(15);
    private final JTextField customerIdField = new JTextField(15);
    private final JTextField customerPhoneField = new JTextField(15);
    private final JComboBox<CustomerType> customerTypeBox = new JComboBox<>(CustomerType.values());
    private final JLabel statusLabel = new JLabel(" ");

    public PurchasePanel(ServerConnection connection) {
        this.connection = connection;

        setLayout(new BorderLayout(10, 10));
        add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        // Clicking a row fills in its product id, so the operator doesn't have
        // to retype it — a small usability win, not a new mechanism.
        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            int row = inventoryTable.getSelectedRow();
            if (row >= 0) {
                String cell = (String) inventoryTableModel.getValueAt(row, 0);
                productIdField.setText(cell.split(" - ")[0]);
            }
        });

        refreshInventory();
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);

        addRow(form, c, 0, "מזהה מוצר:", productIdField);
        addRow(form, c, 1, "כמות:", quantityField);
        addRow(form, c, 2, "שם לקוח:", customerNameField);
        addRow(form, c, 3, "ת\"ז לקוח:", customerIdField);
        addRow(form, c, 4, "טלפון לקוח:", customerPhoneField);
        addRow(form, c, 5, "סוג לקוח:", customerTypeBox);

        JButton buyButton = new JButton("בצע רכישה");
        buyButton.addActionListener(e -> attemptPurchase());
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        form.add(buyButton, c);

        statusLabel.setForeground(Color.RED);
        c.gridy = 7;
        form.add(statusLabel, c);

        return form;
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, Component field) {
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = row;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
    }

    private void refreshInventory() {
        try {
            GetInventoryResponse response = connection.getInventory();
            inventoryTableModel.setRowCount(0);
            for (InventoryItem item : response.getItems()) {
                inventoryTableModel.addRow(new Object[]{
                        item.getProductId() + " - " + item.getProductName(),
                        item.getCategory(),
                        String.format("%.2f", item.getPrice()),
                        item.getAvailableQuantity()
                });
            }
        } catch (Exception e) {
            statusLabel.setText("שגיאה בטעינת המלאי: " + e.getMessage());
        }
    }

    private void attemptPurchase() {
        try {
            RecordSaleRequest request = new RecordSaleRequest(
                    customerNameField.getText(),
                    customerIdField.getText(),
                    customerPhoneField.getText(),
                    (CustomerType) customerTypeBox.getSelectedItem(),
                    productIdField.getText(),
                    Integer.parseInt(quantityField.getText())
            );

            RecordSaleResponse response = connection.recordSale(request);

            if (response.isSuccess()) {
                statusLabel.setForeground(new Color(0, 128, 0));
                statusLabel.setText(String.format("נרכש בהצלחה. סכום לתשלום: %.2f", response.getFinalAmount()));
                refreshInventory();
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (NumberFormatException e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("כמות לא תקינה");
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }
}
