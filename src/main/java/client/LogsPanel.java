package client;

import server.GetLogsResponse;

import javax.swing.*;
import java.awt.*;

public class LogsPanel extends JPanel {
    private final ServerConnection connection;
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel(" ");

    public LogsPanel(ServerConnection connection, Runnable onBack) {
        this.connection = connection;

        setLayout(new BorderLayout(10, 10));
        JButton backButton = new JButton("חזרה");
        backButton.addActionListener(e -> onBack.run());
        add(backButton, BorderLayout.NORTH);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

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
            GetLogsResponse response = connection.getLogs();
            if (!response.isSuccess()) {
                logArea.setText("");
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            logArea.setText(String.join("\n", response.getLines()));
            statusLabel.setText(response.getLines().size() + " שורות לוג");
        } catch (Exception e) {
            statusLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }
}
