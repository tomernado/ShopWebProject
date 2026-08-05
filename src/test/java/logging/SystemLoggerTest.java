package logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
