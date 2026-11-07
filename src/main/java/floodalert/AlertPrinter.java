package floodalert;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

/**
 * Prints a {@link RiskResult} in a readable form. The level label is always
 * at the start of the first line, e.g. {@code [DANGER] Phuket}.
 */
public class AlertPrinter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[1;32m";
    private static final String ANSI_YELLOW = "\u001B[1;33m";
    private static final String ANSI_RED = "\u001B[1;31m";

    private final PrintStream out;
    private final boolean useColor;

    /** Prints plain text without colors (suitable for tests and redirected output). */
    public AlertPrinter(PrintStream out) {
        this(out, false);
    }

    /**
     * @param out      where to print, not null
     * @param useColor whether to color the level label with ANSI escape codes
     */
    public AlertPrinter(PrintStream out, boolean useColor) {
        this.out = Objects.requireNonNull(out, "out");
        this.useColor = useColor;
    }

    public void print(RiskResult result) {
        Objects.requireNonNull(result, "result");
        WeatherData data = result.getData();
        out.println(label(result.getLevel()) + " " + data.getLocation());
        out.println(String.format(Locale.ROOT, "  Rain        : %.1f mm", data.getRainMm()));
        out.println(String.format(Locale.ROOT, "  Rain chance : %d %%", data.getRainChancePercent()));
        out.println("  Reason      : " + result.getReason());
        out.flush();
    }

    private String label(RiskLevel level) {
        String label = "[" + level + "]";
        return useColor ? color(level) + label + ANSI_RESET : label;
    }

    private static String color(RiskLevel level) {
        switch (level) {
            case DANGER:
                return ANSI_RED;
            case WARNING:
                return ANSI_YELLOW;
            default:
                return ANSI_GREEN;
        }
    }
}
