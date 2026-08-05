package logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemLogger {
    private static final SystemLogger INSTANCE = new SystemLogger(Path.of("logs", "system.log"));
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFilePath;

    // Public in addition to the Singleton accessor — exists only for tests, so
    // each test can log to its own isolated temp file instead of the shared
    // production log. Same accommodation as ChatDispatcher.resetForTests().
    public SystemLogger(Path logFilePath) {
        this.logFilePath = logFilePath;
        createParentDirectoryIfNeeded();
    }

    public static SystemLogger getInstance() {
        return INSTANCE;
    }

    public synchronized void log(String category, String message) {
        String line = "[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] [" + category + "] " + message;
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath.toFile(), true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Failed to write log entry: " + e.getMessage());
        }
    }

    private void createParentDirectoryIfNeeded() {
        try {
            if (logFilePath.getParent() != null) {
                Files.createDirectories(logFilePath.getParent());
            }
        } catch (IOException e) {
            System.err.println("Could not create log directory: " + e.getMessage());
        }
    }
}
