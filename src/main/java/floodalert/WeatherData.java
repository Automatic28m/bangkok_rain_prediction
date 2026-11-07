package floodalert;

import java.util.Objects;

/** Immutable weather forecast for one location. */
public class WeatherData {

    private final String location;
    private final double rainMm;
    private final int rainChancePercent;

    /**
     * @param location          location name, not blank
     * @param rainMm            forecast rainfall in millimetres, >= 0
     * @param rainChancePercent chance of rain, 0..100
     * @throws IllegalArgumentException if any value is out of range
     */
    public WeatherData(String location, double rainMm, int rainChancePercent) {
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

    @Override
    public String toString() {
        return "WeatherData{location='" + location + "', rainMm=" + rainMm
                + ", rainChancePercent=" + rainChancePercent + "}";
    }
}
