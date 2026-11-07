package floodalert;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Returns fixed sample data. Used for demos, for testing without network
 * access, and as a fallback when the real API is unavailable.
 */
public class MockWeatherProvider implements WeatherProvider {

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
            throw new WeatherException("Location not found: " + location);
        }
        WeatherData weather = data.get(normalize(location));
        if (weather == null) {
            throw new WeatherException("Location not found: " + location);
        }
        return weather;
    }

    private static String normalize(String location) {
        return location.trim().toLowerCase(Locale.ROOT);
    }
}
