package floodalert;

import floodalert.web.RiskController;
import floodalert.web.WebServer;

/**
 * Entry point: loads {@link AppConfig}, wires the domain objects together and
 * starts the web server.
 *
 * <pre>java -jar app.jar</pre>
 */
public class Main {

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        WebServer server = new WebServer(createController(config.tmdApiToken()));
        server.start(config.port());
        System.out.println("Flood Risk Alerter running at http://localhost:" + config.port());
    }

    /** Builds the controller with a live provider (or a stand-in when no token is set). */
    static RiskController createController(String apiToken) {
        WeatherProvider liveProvider = (apiToken == null)
                ? new UnavailableWeatherProvider(AppConfig.TOKEN_KEY + " is not set on the server, "
                        + "add it to .env (or use mock data) and restart")
                : new TmdWeatherProvider(apiToken);
        return new RiskController(liveProvider, new MockWeatherProvider(), new RiskAnalyzer());
    }
}
