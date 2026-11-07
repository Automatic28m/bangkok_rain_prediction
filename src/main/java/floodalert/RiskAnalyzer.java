package floodalert;

import java.util.Locale;
import java.util.Objects;

/**
 * Assesses flood risk from a forecast. Pure logic: no I/O and no network.
 *
 * <p>Rules are checked from top to bottom and the first match wins:
 * <ol>
 *   <li>DANGER: rain >= 90 mm, or rain >= 50 mm and chance >= 80 %</li>
 *   <li>WARNING: rain >= 35 mm, or rain >= 20 mm and chance >= 70 %</li>
 *   <li>SAFE: anything else</li>
 * </ol>
 */
public class RiskAnalyzer {

    static final double DANGER_RAIN_MM = 90.0;
    static final double DANGER_COMBINED_RAIN_MM = 50.0;
    static final int DANGER_COMBINED_CHANCE_PERCENT = 80;

    static final double WARNING_RAIN_MM = 35.0;
    static final double WARNING_COMBINED_RAIN_MM = 20.0;
    static final int WARNING_COMBINED_CHANCE_PERCENT = 70;

    /**
     * @param data the forecast to assess, not null
     * @return the assessed risk; never {@code null}
     */
    public RiskResult analyze(WeatherData data) {
        Objects.requireNonNull(data, "data");
        double rain = data.getRainMm();
        int chance = data.getRainChancePercent();

        if (rain >= DANGER_RAIN_MM) {
            return new RiskResult(data, RiskLevel.DANGER, rainReason(rain, DANGER_RAIN_MM));
        }
        if (rain >= DANGER_COMBINED_RAIN_MM && chance >= DANGER_COMBINED_CHANCE_PERCENT) {
            return new RiskResult(data, RiskLevel.DANGER, combinedReason(
                    rain, DANGER_COMBINED_RAIN_MM, chance, DANGER_COMBINED_CHANCE_PERCENT));
        }
        if (rain >= WARNING_RAIN_MM) {
            return new RiskResult(data, RiskLevel.WARNING, rainReason(rain, WARNING_RAIN_MM));
        }
        if (rain >= WARNING_COMBINED_RAIN_MM && chance >= WARNING_COMBINED_CHANCE_PERCENT) {
            return new RiskResult(data, RiskLevel.WARNING, combinedReason(
                    rain, WARNING_COMBINED_RAIN_MM, chance, WARNING_COMBINED_CHANCE_PERCENT));
        }
        return new RiskResult(data, RiskLevel.SAFE, String.format(Locale.ROOT,
                "Rain %.1f mm and rain chance %d %% are below warning thresholds", rain, chance));
    }

    private static String rainReason(double rain, double threshold) {
        return String.format(Locale.ROOT, "Rain %.1f mm >= %.1f mm", rain, threshold);
    }

    private static String combinedReason(double rain, double rainThreshold,
                                         int chance, int chanceThreshold) {
        return String.format(Locale.ROOT, "Rain %.1f mm >= %.1f mm and rain chance %d %% >= %d %%",
                rain, rainThreshold, chance, chanceThreshold);
    }
}
