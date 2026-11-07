package floodalert;

/** Thrown when a {@link WeatherProvider} does not recognise the requested location. */
public class LocationNotFoundException extends WeatherException {

    private static final long serialVersionUID = 1L;

    public LocationNotFoundException(String message) {
        super(message);
    }
}
