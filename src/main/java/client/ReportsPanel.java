package client;

import reports.JsonReportExporter;
import reports.SalesReport;
import reports.SalesReportLine;
import reports.WordReportExporter;
import server.GetSalesReportResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;

public class ReportsPanel extends JPanel {
    private final ServerConnection connection;
    private final JsonReportExporter jsonExporter = new JsonReportExporter();
    private final WordReportExporter wordExporter = new WordReportExporter();

    private final JComboBox<String> groupByBox = new JComboBox<>(new String[]{"סניף", "מוצר", "קטגוריה"});
    private final JCheckBox todayOnlyBox = new JCheckBox("רק היום");
    private final DefaultTableModel tableModel =
            new DefaultTableModel(new Object[]{"קבוצה", "כמות", "הכנסה"}, 0);
    private final JLabel statusLabel = new JLabel(" ");

    private SalesReport currentReport;

    public ReportsPanel(ServerConnection connection, Runnable onBack) {
        this.connection = connection;

        setLayout(new BorderLayout(10, 10));
        JButton backButton = new JButton("חזרה");
        backButton.addActionListener(e -> onBack.run());
        add(backButton, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);
    }

    private JPanel buildControls() {
        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);

        c.gridx = 0;
        c.gridy = 0;
        controls.add(new JLabel("קיבוץ לפי:"), c);
        c.gridx = 1;
        controls.add(groupByBox, c);
        c.gridx = 2;
        controls.add(todayOnlyBox, c);

        JButton showButton = new JButton("הצג דוח");
        showButton.addActionListener(e -> fetchReport());
        c.gridx = 3;
        controls.add(showButton, c);

        JButton jsonButton = new JButton("ייצוא JSON");
        jsonButton.addActionListener(e -> exportJson());
        c.gridx = 0;
        c.gridy = 1;
        controls.add(jsonButton, c);

        JButton wordButton = new JButton("ייצוא Word");
        wordButton.addActionListener(e -> exportWord());
        c.gridx = 1;
        controls.add(wordButton, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        controls.add(statusLabel, c);

        return controls;
    }

    private String selectedGroupByKey() {
        return switch (groupByBox.getSelectedIndex()) {
            case 0 -> "branch";
            case 1 -> "product";
            default -> "category";
        };
    }

    private void fetchReport() {
        try {
            GetSalesReportResponse response = connection.getSalesReport(selectedGroupByKey(), todayOnlyBox.isSelected());
            if (!response.isSuccess()) {
                currentReport = null;
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(response.getErrorMessage());
                tableModel.setRowCount(0);
                return;
            }

            currentReport = response.getReport();
            tableModel.setRowCount(0);
            for (SalesReportLine line : currentReport.getLines()) {
                tableModel.addRow(new Object[]{
                        line.getKey(), line.getTotalQuantity(), String.format("%.2f", line.getTotalRevenue())
                });
            }
            statusLabel.setForeground(Color.BLACK);
            statusLabel.setText(" ");
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("שגיאת תקשורת: " + e.getMessage());
        }
    }

    private void exportJson() {
        if (currentReport == null) {
            statusLabel.setText("קודם יש להציג דוח");
            return;
        }
        try {
            Path path = Path.of("reports", "report-" + currentReport.getGroupedBy() + ".json");
            jsonExporter.export(currentReport, path);
            statusLabel.setForeground(new Color(0, 128, 0));
            statusLabel.setText("נשמר: " + path);
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("שגיאת ייצוא: " + e.getMessage());
        }
    }

    private void exportWord() {
        if (currentReport == null) {
            statusLabel.setText("קודם יש להציג דוח");
            return;
        }
        try {
            Path path = Path.of("reports", "report-" + currentReport.getGroupedBy() + ".docx");
            wordExporter.export(currentReport, path);
            statusLabel.setForeground(new Color(0, 128, 0));
            statusLabel.setText("נשמר: " + path);
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("שגיאת ייצוא: " + e.getMessage());
        }
    }
}
