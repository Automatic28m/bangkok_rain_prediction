package floodalert;

/** Thrown when a {@link WeatherProvider} cannot return a forecast. */
public class WeatherException extends Exception {

    private static final long serialVersionUID = 1L;

    public WeatherException(String message) {
        super(message);
    }

    public WeatherException(String message, Throwable cause) {
        super(message, cause);
    }
}
