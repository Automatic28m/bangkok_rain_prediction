package floodalert;

import java.util.Objects;

/** Immutable forecast rain for one hour. */
public class HourlyRain {

    private final String time;
    private final double rainMm;

    /**
     * @param time   start of the hour, ISO-8601 with offset, e.g. "2026-11-07T15:00:00+07:00"
     * @param rainMm forecast rain in that hour in millimetres, >= 0
     * @throws IllegalArgumentException if rainMm is negative or time is blank
     */
    public HourlyRain(String time, double rainMm) {
        Objects.requireNonNull(time, "time");
        if (time.isBlank()) {
            throw new IllegalArgumentException("time must not be blank");
        }
        if (Double.isNaN(rainMm) || rainMm < 0) {
            throw new IllegalArgumentException("rainMm must be >= 0 but was " + rainMm);
        }
        this.time = time;
        this.rainMm = rainMm;
    }

    public String getTime() {
        return time;
    }

    public double getRainMm() {
        return rainMm;
    }

    @Override
    public String toString() {
        return "HourlyRain{time='" + time + "', rainMm=" + rainMm + "}";
    }
}
