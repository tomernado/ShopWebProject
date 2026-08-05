package logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemLoggerTest {

    @Test
    void logAppendsAFormattedLineToTheFile(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("test.log");
        SystemLogger logger = new SystemLogger(logFile);

        logger.log("EMPLOYEE", "dana.l logged in");

        String content = Files.readString(logFile);
        assertTrue(content.contains("[EMPLOYEE]"));
        assertTrue(content.contains("dana.l logged in"));
    }

    @Test
    void multipleLogCallsAppendRatherThanOverwrite(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("test.log");
        SystemLogger logger = new SystemLogger(logFile);

        logger.log("EMPLOYEE", "first entry");
        logger.log("EMPLOYEE", "second entry");

        String content = Files.readString(logFile);
        assertTrue(content.contains("first entry"));
        assertTrue(content.contains("second entry"));
    }

    @Test
    void readAllLinesReturnsEveryLoggedEntry(@TempDir Path tempDir) {
        Path logFile = tempDir.resolve("test.log");
        SystemLogger logger = new SystemLogger(logFile);

        logger.log("EMPLOYEE", "first entry");
        logger.log("EMPLOYEE", "second entry");

        var lines = logger.readAllLines();

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("first entry"));
        assertTrue(lines.get(1).contains("second entry"));
    }

    @Test
    void readAllLinesOnAFreshLoggerReturnsEmptyList(@TempDir Path tempDir) {
        SystemLogger logger = new SystemLogger(tempDir.resolve("never-written.log"));

        assertTrue(logger.readAllLines().isEmpty());
    }
}
