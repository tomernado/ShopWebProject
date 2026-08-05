package client;

import model.Branch;
import model.Role;
import server.CreateAccountRequest;
import server.CreateAccountResponse;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdminPanel extends JPanel {
    private final ServerConnection connection;

    private final JTextField idNumberField = new JTextField(15);
    private final JTextField fullNameField = new JTextField(15);
    private final JTextField phoneField = new JTextField(15);
    private final JTextField accountNumberField = new JTextField(15);
    private final JTextField employeeNumberField = new JTextField(15);
    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JComboBox<Role> roleBox = new JComboBox<>(Role.values());
    private final JComboBox<Branch> branchBox;
    private final JLabel statusLabel = new JLabel(" ");

    public AdminPanel(ServerConnection connection, List<Branch> branches) {
        this.connection = connection;
        this.branchBox = new JComboBox<>(branches.toArray(new Branch[0]));

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        addRow(c, 0, "ת\"ז:", idNumberField);
        addRow(c, 1, "שם מלא:", fullNameField);
        addRow(c, 2, "טלפון:", phoneField);
        addRow(c, 3, "מספר חשבון:", accountNumberField);
        addRow(c, 4, "מספר עובד:", employeeNumberField);
        addRow(c, 5, "שם משתמש:", usernameField);
        addRow(c, 6, "סיסמה:", passwordField);
        addRow(c, 7, "תפקיד:", roleBox);
        addRow(c, 8, "סניף:", branchBox);

        JButton createButton = new JButton("צור חשבון");
        createButton.addActionListener(e -> attemptCreateAccount());
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 2;
        add(createButton, c);

        statusLabel.setForeground(Color.RED);
        c.gridy = 10;
        add(statusLabel, c);
    }

    private void addRow(GridBagConstraints c, int row, String label, Component field) {
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = row;
        add(new JLabel(label), c);
        c.gridx = 1;
        add(field, c);
    }

    private void attemptCreateAccount() {
        try {
            Branch selectedBranch = (Branch) branchBox.getSelectedItem();
            CreateAccountRequest request = new CreateAccountRequest(
                    idNumberField.getText(),
                    fullNameField.getText(),
                    phoneField.getText(),
                    accountNumberField.getText(),
                    employeeNumberField.getText(),
                    usernameField.getText(),
                    new String(passwordField.getPassword()),
                    (Role) roleBox.getSelectedItem(),
                    selectedBranch.getBranchId()
            );

            CreateAccountResponse response = connection.createAccount(request);
            statusLabel.setForeground(response.isSuccess() ? new Color(0, 128, 0) : Color.RED);
            statusLabel.setText(response.isSuccess() ? "החשבון נוצר בהצלחה" : response.getErrorMessage());
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }
}
