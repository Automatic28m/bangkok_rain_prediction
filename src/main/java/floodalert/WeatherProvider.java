package floodalert;

/**
 * Source of weather forecasts. Implement this interface to add a new data
 * source (e.g. ThaiWater or BMA) without changing the rest of the program.
 */
public interface WeatherProvider {

    /**
     * Fetches the current forecast for a location.
     *
     * @param location location name, e.g. "Bangkok"
     * @return the forecast; never {@code null}
     * @throws WeatherException if the data cannot be fetched or is incomplete
     */
    WeatherData fetch(String location) throws WeatherException;
}
