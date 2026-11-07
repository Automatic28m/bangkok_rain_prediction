package floodalert.web;

import floodalert.LocationNotFoundException;
import floodalert.WeatherException;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.Objects;

/**
 * HTTP layer: serves the frontend from {@code /public} on the classpath,
 * routes API requests to the controller and maps errors to status codes.
 *
 * <table>
 *   <tr><td>404</td><td>{@link LocationNotFoundException}</td></tr>
 *   <tr><td>502</td><td>any other {@link WeatherException} (timeout, bad token, rate limit)</td></tr>
 * </table>
 */
public class WebServer {

    private final Javalin app;

    public WebServer(RiskController riskController) {
        Objects.requireNonNull(riskController, "riskController");
        app = Javalin.create(config -> config.staticFiles.add("/public", Location.CLASSPATH));
        app.get("/api/risk", riskController::getRisk);
        app.exception(LocationNotFoundException.class,
                (e, ctx) -> ctx.status(404).json(new ErrorResponse(e.getMessage())));
        app.exception(WeatherException.class,
                (e, ctx) -> ctx.status(502).json(new ErrorResponse(e.getMessage())));
    }

    public void start(int port) {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }

    /** The underlying Javalin app, exposed for in-process tests. */
    Javalin app() {
        return app;
    }
}
