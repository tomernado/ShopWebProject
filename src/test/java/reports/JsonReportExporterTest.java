package reports;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonReportExporterTest {
    private final JsonReportExporter exporter = new JsonReportExporter();

    @Test
    void exportedFileParsesBackToTheSameReport(@TempDir Path tempDir) throws Exception {
        SalesReport report = new SalesReport("branch", List.of(
                new SalesReportLine("B1", 3, 21.0),
                new SalesReportLine("B2", 3, 19.5)
        ));
        Path targetFile = tempDir.resolve("report.json");

        exporter.export(report, targetFile);

        assertTrue(Files.exists(targetFile));
        SalesReport parsedBack = new Gson().fromJson(Files.readString(targetFile), SalesReport.class);
        assertEquals("branch", parsedBack.getGroupedBy());
        assertEquals(2, parsedBack.getLines().size());
        assertEquals("B1", parsedBack.getLines().get(0).getKey());
        assertEquals(21.0, parsedBack.getLines().get(0).getTotalRevenue(), 0.0001);
    }
}
