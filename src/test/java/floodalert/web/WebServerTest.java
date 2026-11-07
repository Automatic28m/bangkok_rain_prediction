package floodalert.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import floodalert.MockWeatherProvider;
import floodalert.RiskAnalyzer;
import floodalert.UnavailableWeatherProvider;
import floodalert.WeatherData;
import floodalert.WeatherException;
import io.javalin.testtools.JavalinTest;
import java.util.Map;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class WebServerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Live provider is a fixed mock, so tests never touch the network. */
    private final WebServer server = new WebServer(new RiskController(
            new MockWeatherProvider(Map.of("Nan", new WeatherData("Nan", 10.0, 30))),
            new MockWeatherProvider(),
            new RiskAnalyzer()));

    @Test
    void mockRequestReturnsAssessedRisk() {
        JavalinTest.test(server.app(), (s, client) -> {
            Response response = client.get("/api/risk?location=Bangkok&mock=true");
            assertEquals(200, response.code());
            JsonNode body = JSON.readTree(response.body().string());
            assertEquals("Bangkok", body.get("location").asText());
            assertEquals(42.0, body.get("rainMm").asDouble());
            assertEquals(80, body.get("rainChancePercent").asInt());
            assertEquals("WARNING", body.get("level").asText());
            assertTrue(body.get("reason").asText().contains("42.0"));
            assertEquals(24, body.get("hourly").size());
            assertTrue(body.get("hourly").get(0).get("time").asText().endsWith("+07:00"));
            assertEquals(35.0, body.get("thresholds").get("warningRainMm").asDouble());
            assertEquals(80, body.get("thresholds").get("dangerCombinedChancePercent").asInt());
        });
    }

    @Test
    void requestWithoutMockUsesLiveProvider() {
        JavalinTest.test(server.app(), (s, client) -> {
            Response response = client.get("/api/risk?location=Nan");
            assertEquals(200, response.code());
            assertEquals("SAFE", JSON.readTree(response.body().string()).get("level").asText());
        });
    }

    @Test
    void missingLocationReturns400() {
        JavalinTest.test(server.app(), (s, client) -> {
            Response response = client.get("/api/risk?location=%20");
            assertEquals(400, response.code());
            assertEquals("Missing location", errorOf(response));
        });
    }

    @Test
    void unknownLocationReturns404() {
        JavalinTest.test(server.app(), (s, client) -> {
            Response response = client.get("/api/risk?location=Atlantis&mock=true");
            assertEquals(404, response.code());
            assertEquals("Location not found: Atlantis", errorOf(response));
        });
    }

    @Test
    void weatherErrorReturns502WithMessage() {
        WebServer noToken = new WebServer(new RiskController(
                new UnavailableWeatherProvider("TMD_API_TOKEN is not set"),
                new MockWeatherProvider(), new RiskAnalyzer()));
        JavalinTest.test(noToken.app(), (s, client) -> {
            Response response = client.get("/api/risk?location=Bangkok");
            assertEquals(502, response.code());
            assertEquals("TMD_API_TOKEN is not set", errorOf(response));
        });
    }

    @Test
    void rateLimitMessageReachesTheUser() {
        WebServer limited = new WebServer(new RiskController(
                location -> {
                    throw new WeatherException("Too many requests to weather service");
                },
                new MockWeatherProvider(), new RiskAnalyzer()));
        JavalinTest.test(limited.app(), (s, client) -> {
            Response response = client.get("/api/risk?location=Bangkok");
            assertEquals(502, response.code());
            assertEquals("Too many requests to weather service", errorOf(response));
        });
    }

    @Test
    void servesFrontend() {
        JavalinTest.test(server.app(), (s, client) -> {
            Response response = client.get("/");
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("ระบบแจ้งเตือนความเสี่ยงน้ำท่วม"));
        });
    }

    @Test
    void servesAboutPage() {
        JavalinTest.test(server.app(), (s, client) -> {
            Response response = client.get("/about.html");
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("ความเป็นมาของโครงงาน"));
        });
    }

    private static String errorOf(Response response) throws Exception {
        return JSON.readTree(response.body().string()).get("error").asText();
    }
}
