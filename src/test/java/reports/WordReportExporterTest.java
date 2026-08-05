package reports;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordReportExporterTest {
    private final WordReportExporter exporter = new WordReportExporter();

    @Test
    void exportedDocxContainsTheTitleAndTableData(@TempDir Path tempDir) throws Exception {
        SalesReport report = new SalesReport("product", List.of(
                new SalesReportLine("Milk", 5, 32.5)
        ));
        Path targetFile = tempDir.resolve("report.docx");

        exporter.export(report, targetFile);

        assertTrue(Files.exists(targetFile));

        try (FileInputStream in = new FileInputStream(targetFile.toFile());
             XWPFDocument document = new XWPFDocument(in)) {

            String titleText = document.getParagraphs().get(0).getText();
            assertTrue(titleText.contains("product"));

            XWPFTable table = document.getTables().get(0);
            assertEquals("product", table.getRow(0).getCell(0).getText());
            assertEquals("Milk", table.getRow(1).getCell(0).getText());
            assertEquals("5", table.getRow(1).getCell(1).getText());
            assertEquals("32.50", table.getRow(1).getCell(2).getText());
        }
    }
}
