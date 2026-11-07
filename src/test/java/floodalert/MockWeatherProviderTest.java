package floodalert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockWeatherProviderTest {

    private static final ZonedDateTime START =
            ZonedDateTime.of(2026, 11, 7, 15, 0, 0, 0, ZoneId.of("Asia/Bangkok"));

    @Test
    void profileHas24HoursThatAddUpToTheTotal() {
        for (WeatherData w : List.of(new WeatherData("A", 42.0, 80),
                new WeatherData("B", 5.0, 20), new WeatherData("C", 95.0, 90),
                new WeatherData("D", 0.3, 5))) {
            List<HourlyRain> hourly = MockWeatherProvider.hourlyProfile(w, START);
            assertEquals(24, hourly.size(), w.getLocation());
            double sum = hourly.stream().mapToDouble(HourlyRain::getRainMm).sum();
            assertEquals(w.getRainMm(), Math.round(sum * 10) / 10.0, w.getLocation());
        }
    }

    @Test
    void rainyHoursFollowTheRainChance() {
        List<HourlyRain> hourly =
                MockWeatherProvider.hourlyProfile(new WeatherData("A", 42.0, 50), START);
        long rainy = hourly.stream().filter(h -> h.getRainMm() >= 0.1).count();
        assertEquals(12, rainy);
    }

    @Test
    void noRainMeansAllDryHours() {
        List<HourlyRain> hourly =
                MockWeatherProvider.hourlyProfile(new WeatherData("A", 0.0, 0), START);
        assertTrue(hourly.stream().allMatch(h -> h.getRainMm() == 0.0));
    }

    @Test
    void hoursStartAtTheGivenTimeOneHourApart() {
        List<HourlyRain> hourly =
                MockWeatherProvider.hourlyProfile(new WeatherData("A", 42.0, 80), START);
        assertEquals("2026-11-07T15:00:00+07:00", hourly.get(0).getTime());
        assertEquals("2026-11-08T14:00:00+07:00", hourly.get(23).getTime());
    }
}
