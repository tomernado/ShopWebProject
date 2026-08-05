package reports;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WordReportExporter {

    public void export(SalesReport report, Path targetFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            writeTitle(document, report);
            writeTable(document, report);
            writeToFile(document, targetFile);
        }
    }

    private void writeTitle(XWPFDocument document, SalesReport report) {
        XWPFParagraph title = document.createParagraph();
        XWPFRun titleRun = title.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setText("Sales Report - grouped by " + report.getGroupedBy());
    }

    private void writeTable(XWPFDocument document, SalesReport report) {
        XWPFTable table = document.createTable(report.getLines().size() + 1, 3);

        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText(report.getGroupedBy());
        header.getCell(1).setText("Total Quantity");
        header.getCell(2).setText("Total Revenue");

        for (int i = 0; i < report.getLines().size(); i++) {
            SalesReportLine line = report.getLines().get(i);
            XWPFTableRow row = table.getRow(i + 1);
            row.getCell(0).setText(line.getKey());
            row.getCell(1).setText(String.valueOf(line.getTotalQuantity()));
            row.getCell(2).setText(String.format("%.2f", line.getTotalRevenue()));
        }
    }

    private void writeToFile(XWPFDocument document, Path targetFile) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        try (FileOutputStream out = new FileOutputStream(targetFile.toFile())) {
            document.write(out);
        }
    }
}
