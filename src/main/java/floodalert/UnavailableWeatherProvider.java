package floodalert;

import java.util.Objects;

/**
 * Stands in for a provider that cannot be used, e.g. the TMD provider when no
 * API token is configured. Every fetch fails with the given message, so the
 * rest of the program never has to check for a missing provider.
 */
public class UnavailableWeatherProvider implements WeatherProvider {

    private final String reason;

    /** @param reason message returned to the user on every fetch, not null */
    public UnavailableWeatherProvider(String reason) {
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    @Override
    public WeatherData fetch(String location) throws WeatherException {
        throw new WeatherException(reason);
    }
}
