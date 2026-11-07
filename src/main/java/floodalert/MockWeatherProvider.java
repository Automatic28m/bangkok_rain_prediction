package floodalert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Returns fixed sample data. Used for demos, for testing without network
 * access, and as a fallback when the real API is unavailable.
 */
public class MockWeatherProvider implements WeatherProvider {

    static final int FORECAST_HOURS = 24;
    private static final ZoneId THAI_TIME = ZoneId.of("Asia/Bangkok");

    private final Map<String, WeatherData> data = new HashMap<>();

    /** Default sample data covering all three risk levels. */
    public MockWeatherProvider() {
        this(Map.of(
                "Bangkok", new WeatherData("Bangkok", 42.0, 80),       // WARNING
                "Chiangmai", new WeatherData("Chiangmai", 5.0, 20),    // SAFE
                "Phuket", new WeatherData("Phuket", 95.0, 90)));       // DANGER
    }

    /** @param data forecasts keyed by location name (matched case-insensitively) */
    public MockWeatherProvider(Map<String, WeatherData> data) {
        Objects.requireNonNull(data, "data");
        data.forEach((location, weather) -> this.data.put(normalize(location), weather));
    }

    @Override
    public WeatherData fetch(String location) throws WeatherException {
        if (location == null || location.isBlank()) {
            throw new LocationNotFoundException("Location not found: " + location);
        }
        WeatherData weather = data.get(normalize(location));
        if (weather == null) {
            throw new LocationNotFoundException("Location not found: " + location);
        }
        if (!weather.getHourly().isEmpty()) {
            return weather;
        }
        ZonedDateTime start = ZonedDateTime.now(THAI_TIME).truncatedTo(ChronoUnit.HOURS);
        return new WeatherData(weather.getLocation(), weather.getRainMm(),
                weather.getRainChancePercent(), hourlyProfile(weather, start));
    }

    /**
     * Spreads the total rain over a contiguous block of rainy hours in the
     * middle of the 24-hour window, heaviest in the centre. The number of rainy
     * hours follows the rain chance, and the hourly values add up to the total.
     */
    static List<HourlyRain> hourlyProfile(WeatherData weather, ZonedDateTime start) {
        double total = weather.getRainMm();
        int rainyHours = total <= 0 ? 0 : Math.max(1, Math.min(FORECAST_HOURS,
                (int) Math.round(weather.getRainChancePercent() * FORECAST_HOURS / 100.0)));
        int first = (FORECAST_HOURS - rainyHours) / 2;

        double[] rain = new double[FORECAST_HOURS];
        double weightSum = 0;
        for (int k = 0; k < rainyHours; k++) {
            weightSum += Math.sin(Math.PI * (k + 0.5) / rainyHours);
        }
        double rounded = 0;
        int peak = first;
        for (int k = 0; k < rainyHours; k++) {
            double share = total * Math.sin(Math.PI * (k + 0.5) / rainyHours) / weightSum;
            rain[first + k] = Math.max(0.1, Math.round(share * 10) / 10.0);
            rounded += rain[first + k];
            if (rain[first + k] > rain[peak]) {
                peak = first + k;
            }
        }
        if (rainyHours > 0) {
            // put the rounding remainder on the peak hour so the hours add up to the total
            rain[peak] = Math.max(0.1, Math.round((rain[peak] + total - rounded) * 10) / 10.0);
        }

        List<HourlyRain> hourly = new ArrayList<>(FORECAST_HOURS);
        for (int h = 0; h < FORECAST_HOURS; h++) {
            hourly.add(new HourlyRain(
                    start.plusHours(h).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), rain[h]));
        }
        return hourly;
    }

    private static String normalize(String location) {
        return location.trim().toLowerCase(Locale.ROOT);
    }
}
