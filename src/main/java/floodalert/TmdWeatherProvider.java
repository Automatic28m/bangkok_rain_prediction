package floodalert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Fetches a 24-hour forecast from the TMD (Thai Meteorological Department)
 * hourly forecast API.
 *
 * <p>TMD has no chance-of-rain field, so values are derived from the hourly data:
 * {@code rainMm} is the total rain over the next 24 hours and
 * {@code rainChancePercent} is the share of those hours with at least 0.1 mm of rain.
 */
public class TmdWeatherProvider implements WeatherProvider {

    private static final String ENDPOINT =
            "https://data.tmd.go.th/nwpapi/v1/forecast/location/hourly/at";
    private static final ZoneId THAI_TIME = ZoneId.of("Asia/Bangkok");
    private static final int FORECAST_HOURS = 24;
    private static final double RAINY_HOUR_MIN_MM = 0.1;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String apiToken;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public TmdWeatherProvider(String apiToken) {
        this(apiToken, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    public TmdWeatherProvider(String apiToken, HttpClient client) {
        this.apiToken = Objects.requireNonNull(apiToken, "apiToken");
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public WeatherData fetch(String location) throws WeatherException {
        double[] latLon = resolve(location);
        HttpResponse<String> response = send(buildRequest(latLon[0], latLon[1]));

        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new WeatherException("Invalid or missing API token");
        }
        if (status == 422) {
            throw new WeatherException("Location not found: " + location);
        }
        if (status == 429) {
            // TMD allows roughly 60 requests per minute per token
            throw new WeatherException(
                    "Too many requests to weather service, please wait a minute and try again");
        }
        if (status != 200) {
            throw new WeatherException("Weather service returned HTTP " + status);
        }
        return parse(location, response.body());
    }

    private HttpRequest buildRequest(double lat, double lon) {
        ZonedDateTime now = ZonedDateTime.now(THAI_TIME);
        String query = "lat=" + lat
                + "&lon=" + lon
                + "&fields=rain"
                + "&date=" + now.toLocalDate()
                + "&hour=" + now.getHour()
                + "&duration=" + FORECAST_HOURS;
        return HttpRequest.newBuilder(URI.create(ENDPOINT + "?" + query))
                .timeout(TIMEOUT)
                .header("accept", "application/json")
                .header("authorization", "Bearer " + apiToken)
                .GET()
                .build();
    }

    private HttpResponse<String> send(HttpRequest request) throws WeatherException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new WeatherException("Cannot reach weather service (timeout)", e);
        } catch (IOException e) {
            throw new WeatherException("Cannot reach weather service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherException("Request was interrupted", e);
        }
    }

    /** Turns the hourly JSON response into a 24-hour summary. */
    WeatherData parse(String location, String json) throws WeatherException {
        JsonNode forecasts;
        try {
            forecasts = mapper.readTree(json).path("WeatherForecasts").path(0).path("forecasts");
        } catch (IOException e) {
            throw new WeatherException("Unexpected response from weather service", e);
        }
        if (!forecasts.isArray() || forecasts.isEmpty()) {
            throw new WeatherException("Unexpected response from weather service");
        }

        double totalRain = 0;
        int rainyHours = 0;
        for (JsonNode hour : forecasts) {
            JsonNode rain = hour.path("data").path("rain");
            if (!rain.isNumber()) {
                throw new WeatherException("Unexpected response from weather service");
            }
            totalRain += rain.asDouble();
            if (rain.asDouble() >= RAINY_HOUR_MIN_MM) {
                rainyHours++;
            }
        }

        double rainMm = Math.round(totalRain * 10) / 10.0;
        int rainChancePercent = (int) Math.round(rainyHours * 100.0 / forecasts.size());
        return new WeatherData(location.trim(), rainMm, rainChancePercent);
    }

    /** Accepts a name known to {@link ThaiLocations} or "lat,lon". */
    private static double[] resolve(String location) throws WeatherException {
        if (location == null || location.isBlank()) {
            throw new WeatherException("Location not found: " + location);
        }
        double[] known = ThaiLocations.find(location);
        if (known != null) {
            return known;
        }
        String[] parts = location.split(",");
        if (parts.length == 2) {
            try {
                return new double[] {
                        Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
            } catch (NumberFormatException ignored) {
                // fall through to "not found"
            }
        }
        throw new WeatherException("Location not found: " + location);
    }
}
