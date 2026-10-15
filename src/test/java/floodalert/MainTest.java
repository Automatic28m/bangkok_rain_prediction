package floodalert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MainTest {

    private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

    private int run(String token, String... args) {
        return Main.run(args, token, out, err, false);
    }

    private String stdout() {
        return outBuffer.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return errBuffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void mockRunPrintsAlertAndSucceeds() {
        assertEquals(Main.EXIT_OK, run(null, "Phuket", "--mock"));
        assertTrue(stdout().startsWith("[DANGER] Phuket"), stdout());
        assertEquals("", stderr());
    }

    @Test
    void mockFlagMayComeFirst() {
        assertEquals(Main.EXIT_OK, run(null, "--mock", "chiangmai"));
        assertTrue(stdout().startsWith("[SAFE] Chiangmai"), stdout());
    }

    @Test
    void missingLocationIsUsageError() {
        assertEquals(Main.EXIT_USAGE, run(null, "--mock"));
        assertTrue(stderr().contains(Main.USAGE), stderr());
        assertEquals("", stdout());
    }

    @Test
    void noArgumentsIsUsageError() {
        assertEquals(Main.EXIT_USAGE, run("token"));
    }

    @Test
    void tooManyArgumentsIsUsageError() {
        assertEquals(Main.EXIT_USAGE, run(null, "Bangkok", "Phuket", "--mock"));
        assertTrue(stderr().contains("Too many arguments"), stderr());
    }

    @Test
    void unknownOptionIsUsageError() {
        assertEquals(Main.EXIT_USAGE, run(null, "Bangkok", "--mok"));
        assertTrue(stderr().contains("Unknown option: --mok"), stderr());
    }

    @Test
    void missingTokenIsUsageError() {
        assertEquals(Main.EXIT_USAGE, run(null, "Bangkok"));
        assertTrue(stderr().contains(Main.TOKEN_ENV), stderr());
    }

    @Test
    void blankTokenIsUsageError() {
        assertEquals(Main.EXIT_USAGE, run("   ", "Bangkok"));
    }

    @Test
    void unknownMockLocationIsWeatherError() {
        assertEquals(Main.EXIT_WEATHER_ERROR, run(null, "Atlantis", "--mock"));
        assertTrue(stderr().contains("Location not found: Atlantis"), stderr());
        assertEquals("", stdout());
    }

    @Test
    void unknownTmdLocationFailsBeforeAnyNetworkCall() {
        // TmdWeatherProvider rejects names not in its table without calling the API
        assertEquals(Main.EXIT_WEATHER_ERROR, run("dummy-token", "Atlantis"));
        assertTrue(stderr().contains("Location not found: Atlantis"), stderr());
    }

    @Test
    void wiresProviderAnalyzerAndPrinter() {
        WeatherProvider provider = location -> new WeatherData(location, 42.0, 80);
        int code = Main.run("Bangkok", provider, new RiskAnalyzer(), new AlertPrinter(out), err);

        assertEquals(Main.EXIT_OK, code);
        assertTrue(stdout().startsWith("[WARNING] Bangkok"), stdout());
    }
}
