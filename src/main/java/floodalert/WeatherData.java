package floodalert;

import java.util.List;
import java.util.Objects;

/** Immutable weather forecast for one location. */
public class WeatherData {

    private final String location;
    private final double rainMm;
    private final int rainChancePercent;
    private final List<HourlyRain> hourly;

    /** Summary only, without an hourly breakdown. */
    public WeatherData(String location, double rainMm, int rainChancePercent) {
        this(location, rainMm, rainChancePercent, List.of());
    }

    /**
     * @param location          location name, not blank
     * @param rainMm            forecast rainfall in millimetres, >= 0
     * @param rainChancePercent chance of rain, 0..100
     * @param hourly            hour-by-hour forecast in time order, may be empty, not null
     * @throws IllegalArgumentException if any value is out of range
     */
    public WeatherData(String location, double rainMm, int rainChancePercent,
                       List<HourlyRain> hourly) {
        Objects.requireNonNull(location, "location");
        if (location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
        if (Double.isNaN(rainMm) || rainMm < 0) {
            throw new IllegalArgumentException("rainMm must be >= 0 but was " + rainMm);
        }
        if (rainChancePercent < 0 || rainChancePercent > 100) {
            throw new IllegalArgumentException(
                    "rainChancePercent must be 0..100 but was " + rainChancePercent);
        }
        this.location = location;
        this.rainMm = rainMm;
        this.rainChancePercent = rainChancePercent;
        this.hourly = List.copyOf(Objects.requireNonNull(hourly, "hourly"));
    }

    public String getLocation() {
        return location;
    }

    public double getRainMm() {
        return rainMm;
    }

    public int getRainChancePercent() {
        return rainChancePercent;
    }

    /** @return the hourly breakdown in time order; empty when the source has none */
    public List<HourlyRain> getHourly() {
        return hourly;
    }

    @Override
    public String toString() {
        return "WeatherData{location='" + location + "', rainMm=" + rainMm
                + ", rainChancePercent=" + rainChancePercent + "}";
    }
}
