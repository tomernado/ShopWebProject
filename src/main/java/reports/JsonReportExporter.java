package reports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonReportExporter {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void export(SalesReport report, Path targetFile) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        Files.writeString(targetFile, gson.toJson(report));
    }
}
