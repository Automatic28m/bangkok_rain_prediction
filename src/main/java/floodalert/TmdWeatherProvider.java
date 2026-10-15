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
import java.util.Locale;
import java.util.Map;
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

    /** Known locations; keys are normalized names (lowercase, no spaces, '-' or '_'). */
    private static final Map<String, double[]> COORDINATES = Map.ofEntries(
            Map.entry("bangkok", new double[] {13.7563, 100.5018}),
            Map.entry("nonthaburi", new double[] {13.8621, 100.5144}),
            Map.entry("pathumthani", new double[] {14.0208, 100.5250}),
            Map.entry("samutprakan", new double[] {13.5991, 100.5998}),
            Map.entry("ayutthaya", new double[] {14.3532, 100.5689}),
            Map.entry("chiangmai", new double[] {18.7883, 98.9853}),
            Map.entry("chiangrai", new double[] {19.9105, 99.8406}),
            Map.entry("khonkaen", new double[] {16.4322, 102.8236}),
            Map.entry("nakhonratchasima", new double[] {14.9799, 102.0978}),
            Map.entry("udonthani", new double[] {17.4138, 102.7870}),
            Map.entry("ubonratchathani", new double[] {15.2448, 104.8473}),
            Map.entry("chonburi", new double[] {13.3611, 100.9847}),
            Map.entry("pattaya", new double[] {12.9236, 100.8825}),
            Map.entry("phuket", new double[] {7.8804, 98.3923}),
            Map.entry("suratthani", new double[] {9.1382, 99.3215}),
            Map.entry("hatyai", new double[] {7.0086, 100.4747}),
            Map.entry("songkhla", new double[] {7.1898, 100.5954}));

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

    /** Accepts a known location name or "lat,lon". */
    private static double[] resolve(String location) throws WeatherException {
        if (location == null || location.isBlank()) {
            throw new WeatherException("Location not found: " + location);
        }
        double[] known = COORDINATES.get(normalize(location));
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

    private static String normalize(String location) {
        return location.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]", "");
    }
}
