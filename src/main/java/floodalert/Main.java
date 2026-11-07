package floodalert;

import floodalert.web.RiskController;
import floodalert.web.WebServer;

/**
 * Entry point: wires the domain objects together and starts the web server.
 *
 * <pre>java -jar app.jar</pre>
 *
 * Environment variables:
 * <ul>
 *   <li>{@code TMD_API_TOKEN} - token for live TMD data (mock data works without it)</li>
 *   <li>{@code PORT} - HTTP port, default 8080</li>
 * </ul>
 */
public class Main {

    static final String TOKEN_ENV = "TMD_API_TOKEN";
    static final String PORT_ENV = "PORT";
    static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = parsePort(System.getenv(PORT_ENV));
        WebServer server = new WebServer(createController(System.getenv(TOKEN_ENV)));
        server.start(port);
        System.out.println("Flood Risk Alerter running at http://localhost:" + port);
    }

    /** Builds the controller with a live provider (or a stand-in when no token is set). */
    static RiskController createController(String apiToken) {
        WeatherProvider liveProvider = (apiToken == null || apiToken.isBlank())
                ? new UnavailableWeatherProvider(TOKEN_ENV + " is not set on the server, "
                        + "use mock data or set the token and restart")
                : new TmdWeatherProvider(apiToken.trim());
        return new RiskController(liveProvider, new MockWeatherProvider(), new RiskAnalyzer());
    }

    static int parsePort(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(PORT_ENV + " must be a number but was " + value, e);
        }
    }
}
