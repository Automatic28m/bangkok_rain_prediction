# Class Diagram Review

The diagram matches `develop`, but not `feature/ab-web-app`.

## Remove
- `AlertPrinter` and its arrows (the CLI was replaced by the web app)

## Add
| Class | Relationship |
|-------|--------------|
| `LocationNotFoundException` | extends `WeatherException`; thrown by both providers |
| `UnavailableWeatherProvider` | implements `WeatherProvider` (used when no token is set) |
| `AppConfig` | reads `TMD_API_TOKEN` and `PORT` from `.env` |
| `web.RiskController` | uses `WeatherProvider` (live + mock) and `RiskAnalyzer` |
| `web.WebServer` | depends on `RiskController`; maps exceptions to HTTP 404/502 |
| `web.RiskResponse`, `web.ErrorResponse` | «record», the JSON replies |
| `HourlyRain` *(uncommitted)* | composition: `WeatherData` 1 → * `HourlyRain` |

## Update
- `WeatherData`: add `-hourly : List<HourlyRain>` and `getHourly()` *(uncommitted)*
- `RiskAnalyzer`: threshold constants change from `~` to `+` *(uncommitted)*
- Underline static members, and mark `ThaiLocations` as package-private and `final`
