package floodalert;

import java.util.Objects;

/** Immutable outcome of a risk assessment: the input data, the level and why. */
public class RiskResult {

    private final WeatherData data;
    private final RiskLevel level;
    private final String reason;

    /**
     * @param data   the forecast that was assessed, not null
     * @param level  the assessed risk level, not null
     * @param reason human-readable explanation, not blank
     * @throws IllegalArgumentException if reason is blank
     */
    public RiskResult(WeatherData data, RiskLevel level, String reason) {
        this.data = Objects.requireNonNull(data, "data");
        this.level = Objects.requireNonNull(level, "level");
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        this.reason = reason;
    }

    public WeatherData getData() {
        return data;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "RiskResult{level=" + level + ", reason='" + reason + "', data=" + data + "}";
    }
}
