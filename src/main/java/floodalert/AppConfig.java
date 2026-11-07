package floodalert;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Application settings, read from a {@code .env} file in the working directory.
 * A real environment variable with the same name overrides the file, and the
 * file is optional.
 *
 * <ul>
 *   <li>{@code TMD_API_TOKEN} - token for live TMD data (mock data works without it)</li>
 *   <li>{@code PORT} - HTTP port, default 8080</li>
 * </ul>
 */
public class AppConfig {

    static final String TOKEN_KEY = "TMD_API_TOKEN";
    static final String PORT_KEY = "PORT";
    static final int DEFAULT_PORT = 8080;

    private final Dotenv env;

    AppConfig(Dotenv env) {
        this.env = Objects.requireNonNull(env, "env");
    }

    /** Loads {@code .env} from the current working directory, if it exists. */
    public static AppConfig load() {
        return load(Path.of("."));
    }

    /** Loads {@code .env} from the given directory, if it exists. */
    static AppConfig load(Path directory) {
        return new AppConfig(Dotenv.configure()
                .directory(directory.toString())
                .ignoreIfMissing()
                .load());
    }

    /** @return the trimmed token, or {@code null} when it is not set or blank */
    public String tmdApiToken() {
        String token = env.get(TOKEN_KEY);
        return (token == null || token.isBlank()) ? null : token.trim();
    }

    /** @throws IllegalArgumentException if {@code PORT} is set but not a number */
    public int port() {
        String value = env.get(PORT_KEY);
        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(PORT_KEY + " must be a number but was " + value, e);
        }
    }
}
