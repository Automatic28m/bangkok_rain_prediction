package floodalert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskAnalyzerTest {

    private final RiskAnalyzer analyzer = new RiskAnalyzer();

    private RiskResult analyze(double rainMm, int rainChancePercent) {
        return analyzer.analyze(new WeatherData("Bangkok", rainMm, rainChancePercent));
    }

    // The five cases required by CONTRACT.md section 3.3
    @Test
    void lightRainIsSafe() {
        assertEquals(RiskLevel.SAFE, analyze(5.0, 20).getLevel());
    }

    @Test
    void heavyRainBoundaryIsWarning() {
        RiskResult result = analyze(35.0, 0);
        assertEquals(RiskLevel.WARNING, result.getLevel());
        assertEquals("Rain 35.0 mm >= 35.0 mm", result.getReason());
    }

    @Test
    void moderateRainWithHighChanceIsWarning() {
        RiskResult result = analyze(25.0, 80);
        assertEquals(RiskLevel.WARNING, result.getLevel());
        assertEquals("Rain 25.0 mm >= 20.0 mm and rain chance 80 % >= 70 %", result.getReason());
    }

    @Test
    void veryHeavyRainIsDanger() {
        RiskResult result = analyze(95.0, 30);
        assertEquals(RiskLevel.DANGER, result.getLevel());
        assertEquals("Rain 95.0 mm >= 90.0 mm", result.getReason());
    }

    @Test
    void heavyRainWithVeryHighChanceIsDanger() {
        RiskResult result = analyze(55.0, 90);
        assertEquals(RiskLevel.DANGER, result.getLevel());
        assertEquals("Rain 55.0 mm >= 50.0 mm and rain chance 90 % >= 80 %", result.getReason());
    }

    // Boundaries just below / at each threshold
    @ParameterizedTest(name = "{0} mm, {1} % -> {2}")
    @CsvSource({
            "0.0,   0,   SAFE",
            "34.9,  69,  SAFE",
            "19.9,  100, SAFE",
            "20.0,  69,  SAFE",
            "20.0,  70,  WARNING",
            "49.9,  100, WARNING",
            "50.0,  79,  WARNING",
            "50.0,  80,  DANGER",
            "89.9,  0,   WARNING",
            "90.0,  0,   DANGER",
    })
    void thresholdBoundaries(double rainMm, int chance, RiskLevel expected) {
        assertEquals(expected, analyze(rainMm, chance).getLevel());
    }

    @Test
    void resultKeepsSourceData() {
        WeatherData data = new WeatherData("Phuket", 95.0, 90);
        assertSame(data, analyzer.analyze(data).getData());
    }

    @Test
    void safeResultHasReason() {
        assertEquals("Rain 5.0 mm and rain chance 20 % are below warning thresholds",
                analyze(5.0, 20).getReason());
    }

    @Test
    void nullDataIsRejected() {
        assertThrows(NullPointerException.class, () -> analyzer.analyze(null));
    }

    @Test
    void mockDataCoversAllThreeLevels() throws WeatherException {
        MockWeatherProvider mock = new MockWeatherProvider();
        assertEquals(RiskLevel.SAFE, analyzer.analyze(mock.fetch("Chiangmai")).getLevel());
        assertEquals(RiskLevel.WARNING, analyzer.analyze(mock.fetch("Bangkok")).getLevel());
        assertEquals(RiskLevel.DANGER, analyzer.analyze(mock.fetch("Phuket")).getLevel());
    }
}
