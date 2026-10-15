package floodalert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AlertPrinterTest {

    private static final String NL = System.lineSeparator();

    private static String print(RiskResult result, boolean useColor) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new AlertPrinter(out, useColor).print(result);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printsContractFormat() {
        RiskResult result = new RiskResult(new WeatherData("Phuket", 95.0, 90),
                RiskLevel.DANGER, "Rain 95.0 mm >= 90.0 mm");

        assertEquals("[DANGER] Phuket" + NL
                + "  Rain        : 95.0 mm" + NL
                + "  Rain chance : 90 %" + NL
                + "  Reason      : Rain 95.0 mm >= 90.0 mm" + NL, print(result, false));
    }

    @Test
    void colorWrapsOnlyTheLabel() {
        RiskResult result = new RiskResult(new WeatherData("Bangkok", 42.0, 80),
                RiskLevel.WARNING, "Rain 42.0 mm >= 35.0 mm");

        String output = print(result, true);
        assertTrue(output.startsWith("\u001B[1;33m[WARNING]\u001B[0m Bangkok" + NL), output);
    }
}
