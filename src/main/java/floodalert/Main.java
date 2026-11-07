package floodalert;

import java.io.PrintStream;

/**
 * Command-line entry point.
 *
 * <pre>java -jar app.jar &lt;location&gt; [--mock]</pre>
 *
 * Exit codes: 0 success, 1 invalid arguments or missing token, 2 weather data error.
 */
public class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_USAGE = 1;
    static final int EXIT_WEATHER_ERROR = 2;

    static final String TOKEN_ENV = "TMD_API_TOKEN";
    static final String MOCK_FLAG = "--mock";
    static final String USAGE = "Usage: java -jar app.jar <location> [--mock]";

    public static void main(String[] args) {
        boolean useColor = System.console() != null && System.getenv("NO_COLOR") == null;
        int exitCode = run(args, System.getenv(TOKEN_ENV), System.out, System.err, useColor);
        System.exit(exitCode);
    }

    /**
     * Runs the program without exiting the JVM, so it can be tested.
     *
     * @param args     command-line arguments
     * @param apiToken value of {@code TMD_API_TOKEN}; may be null when {@code --mock} is used
     * @return the process exit code
     */
    static int run(String[] args, String apiToken, PrintStream out, PrintStream err,
                   boolean useColor) {
        String location = null;
        boolean mock = false;
        for (String arg : args) {
            if (MOCK_FLAG.equals(arg)) {
                mock = true;
            } else if (arg.startsWith("--")) {
                return usageError(err, "Unknown option: " + arg);
            } else if (location != null) {
                return usageError(err, "Too many arguments: " + arg);
            } else {
                location = arg;
            }
        }
        if (location == null || location.isBlank()) {
            return usageError(err, "Missing <location>");
        }

        WeatherProvider provider;
        if (mock) {
            provider = new MockWeatherProvider();
        } else if (apiToken == null || apiToken.isBlank()) {
            return usageError(err, TOKEN_ENV + " is not set (use " + MOCK_FLAG + " to run without it)");
        } else {
            provider = new TmdWeatherProvider(apiToken.trim());
        }

        return run(location, provider, new RiskAnalyzer(), new AlertPrinter(out, useColor), err);
    }

    /** Fetches, analyzes and prints using the given collaborators. */
    static int run(String location, WeatherProvider provider, RiskAnalyzer analyzer,
                   AlertPrinter printer, PrintStream err) {
        try {
            WeatherData data = provider.fetch(location);
            printer.print(analyzer.analyze(data));
            return EXIT_OK;
        } catch (WeatherException e) {
            err.println("Error: " + e.getMessage());
            return EXIT_WEATHER_ERROR;
        }
    }

    private static int usageError(PrintStream err, String message) {
        err.println("Error: " + message);
        err.println(USAGE);
        return EXIT_USAGE;
    }
}
