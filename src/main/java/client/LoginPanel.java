package client;

import model.Employee;
import server.LoginResponse;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private final ServerConnection connection;
    private final Consumer<Employee> onLoginSuccess;

    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JLabel errorLabel = new JLabel(" ");

    public LoginPanel(ServerConnection connection, Consumer<Employee> onLoginSuccess) {
        this.connection = connection;
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel("שם משתמש:"), c);
        c.gridx = 1;
        add(usernameField, c);

        c.gridx = 0;
        c.gridy = 1;
        add(new JLabel("סיסמה:"), c);
        c.gridx = 1;
        add(passwordField, c);

        JButton loginButton = new JButton("התחבר");
        loginButton.addActionListener(e -> attemptLogin());
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        add(loginButton, c);

        errorLabel.setForeground(Color.RED);
        c.gridy = 3;
        add(errorLabel, c);
    }

    private void attemptLogin() {
        try {
            LoginResponse response = connection.login(usernameField.getText(), new String(passwordField.getPassword()));
            if (response.isSuccess()) {
                errorLabel.setText(" ");
                onLoginSuccess.accept(response.getEmployee());
            } else {
                errorLabel.setText(response.getErrorMessage());
            }
        } catch (Exception e) {
            errorLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }
}
