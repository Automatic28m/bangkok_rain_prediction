package floodalert.web;

import floodalert.RiskAnalyzer;
import floodalert.WeatherException;
import floodalert.WeatherProvider;
import io.javalin.http.Context;
import java.util.Objects;

/**
 * Handles {@code GET /api/risk?location=<name>[&mock=true]}.
 *
 * <p>Fetches the forecast from the live or mock provider, assesses it and
 * returns a {@link RiskResponse}. Weather errors are thrown on to
 * {@link WebServer}, which turns them into error responses.
 */
public class RiskController {

    static final String LOCATION_PARAM = "location";
    static final String MOCK_PARAM = "mock";

    private final WeatherProvider liveProvider;
    private final WeatherProvider mockProvider;
    private final RiskAnalyzer analyzer;

    public RiskController(WeatherProvider liveProvider, WeatherProvider mockProvider,
                          RiskAnalyzer analyzer) {
        this.liveProvider = Objects.requireNonNull(liveProvider, "liveProvider");
        this.mockProvider = Objects.requireNonNull(mockProvider, "mockProvider");
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    }

    public void getRisk(Context ctx) throws WeatherException {
        String location = ctx.queryParam(LOCATION_PARAM);
        if (location == null || location.isBlank()) {
            ctx.status(400).json(new ErrorResponse("Missing location"));
            return;
        }
        WeatherProvider provider = "true".equalsIgnoreCase(ctx.queryParam(MOCK_PARAM))
                ? mockProvider
                : liveProvider;
        ctx.json(RiskResponse.from(analyzer.analyze(provider.fetch(location.trim()))));
    }
}
