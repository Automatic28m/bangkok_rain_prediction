package floodalert.web;

import floodalert.RiskResult;
import floodalert.WeatherData;

/** JSON body of a successful {@code /api/risk} response. */
public record RiskResponse(String location, double rainMm, int rainChancePercent,
                           String level, String reason) {

    public static RiskResponse from(RiskResult result) {
        WeatherData data = result.getData();
        return new RiskResponse(data.getLocation(), data.getRainMm(),
                data.getRainChancePercent(), result.getLevel().name(), result.getReason());
    }
}
