package floodalert.web;

import floodalert.HourlyRain;
import floodalert.RiskAnalyzer;
import floodalert.RiskResult;
import floodalert.WeatherData;
import java.util.List;

/** JSON body of a successful {@code /api/risk} response. */
public record RiskResponse(String location, double rainMm, int rainChancePercent,
                           String level, String reason, List<Hour> hourly,
                           Thresholds thresholds) {

    /** Forecast rain for one hour. */
    public record Hour(String time, double rainMm) {
    }

    /** The rules {@link RiskAnalyzer} applies, so clients never hardcode them. */
    public record Thresholds(double warningRainMm, double warningCombinedRainMm,
                             int warningCombinedChancePercent, double dangerRainMm,
                             double dangerCombinedRainMm, int dangerCombinedChancePercent) {

        static final Thresholds CURRENT = new Thresholds(
                RiskAnalyzer.WARNING_RAIN_MM, RiskAnalyzer.WARNING_COMBINED_RAIN_MM,
                RiskAnalyzer.WARNING_COMBINED_CHANCE_PERCENT, RiskAnalyzer.DANGER_RAIN_MM,
                RiskAnalyzer.DANGER_COMBINED_RAIN_MM, RiskAnalyzer.DANGER_COMBINED_CHANCE_PERCENT);
    }

    public static RiskResponse from(RiskResult result) {
        WeatherData data = result.getData();
        List<Hour> hourly = data.getHourly().stream()
                .map((HourlyRain h) -> new Hour(h.getTime(), h.getRainMm()))
                .toList();
        return new RiskResponse(data.getLocation(), data.getRainMm(),
                data.getRainChancePercent(), result.getLevel().name(), result.getReason(),
                hourly, Thresholds.CURRENT);
    }
}
