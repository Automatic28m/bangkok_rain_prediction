package floodalert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigTest {

    // Keys use names unlikely to exist in the real environment, which would override the file.
    @TempDir
    Path dir;

    @Test
    void readsTokenAndPortFromEnvFile() throws Exception {
        Files.writeString(dir.resolve(".env"),
                "# comment\nTMD_API_TOKEN=\"  eyJabc  \"\nPORT=9090\n");
        AppConfig config = AppConfig.load(dir);
        assertEquals("eyJabc", config.tmdApiToken());
        assertEquals(9090, config.port());
    }

    @Test
    void missingFileUsesDefaults() {
        AppConfig config = AppConfig.load(dir);
        if (System.getenv(AppConfig.TOKEN_KEY) == null) {
            assertNull(config.tmdApiToken());
        }
        if (System.getenv(AppConfig.PORT_KEY) == null) {
            assertEquals(AppConfig.DEFAULT_PORT, config.port());
        }
    }

    @Test
    void blankTokenCountsAsNotSet() throws Exception {
        Files.writeString(dir.resolve(".env"), "TMD_API_TOKEN=\n");
        if (System.getenv(AppConfig.TOKEN_KEY) == null) {
            assertNull(AppConfig.load(dir).tmdApiToken());
        }
    }

    @Test
    void invalidPortIsRejected() throws Exception {
        Files.writeString(dir.resolve(".env"), "PORT=abc\n");
        if (System.getenv(AppConfig.PORT_KEY) == null) {
            assertThrows(IllegalArgumentException.class, () -> AppConfig.load(dir).port());
        }
    }
}
