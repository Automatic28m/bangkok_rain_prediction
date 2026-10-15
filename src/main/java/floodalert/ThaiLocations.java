package floodalert;

import java.util.Locale;
import java.util.Map;

/**
 * Coordinates of all 77 Thai provinces (plus a few well-known cities), looked
 * up by English name.
 *
 * <p>Province coordinates are the reference points returned by the TMD
 * {@code forecast/location/daily/region} endpoint. Names are matched
 * case-insensitively and ignoring spaces, '-' and '_', so "Chiang Mai",
 * "chiang-mai" and "CHIANGMAI" are the same.
 */
final class ThaiLocations {

    private static final Map<String, double[]> COORDINATES = Map.ofEntries(
            // Central
            entry("Bangkok", 13.753958, 100.501746),
            entry("Kamphaeng Phet", 16.483852, 99.520335),
            entry("Chai Nat", 15.185492, 100.123905),
            entry("Nakhon Nayok", 14.203659, 101.217199),
            entry("Nakhon Pathom", 13.819463, 100.066009),
            entry("Nakhon Sawan", 15.704984, 100.137608),
            entry("Nonthaburi", 13.862329, 100.513434),
            entry("Pathum Thani", 14.019328, 100.534684),
            entry("Phra Nakhon Si Ayutthaya", 14.334025, 100.611121),
            entry("Ayutthaya", 14.334025, 100.611121),
            entry("Phichit", 16.438631, 100.350259),
            entry("Phitsanulok", 16.824740, 100.259211),
            entry("Lop Buri", 14.798047, 100.653718),
            entry("Samut Prakan", 13.600377, 100.597007),
            entry("Samut Songkhram", 13.413943, 100.003115),
            entry("Samut Sakhon", 13.548337, 100.273428),
            entry("Saraburi", 14.527247, 100.909569),
            entry("Sing Buri", 14.888025, 100.404495),
            entry("Suphan Buri", 14.474494, 100.118495),
            entry("Sukhothai", 17.007681, 99.822841),
            entry("Uthai Thani", 15.379404, 100.024822),
            entry("Ang Thong", 14.589071, 100.453113),
            entry("Phetchabun", 16.442954, 101.149147),
            // North
            entry("Nan", 18.794041, 100.731844),
            entry("Phayao", 19.191901, 99.878510),
            entry("Lampang", 18.291949, 99.504297),
            entry("Lamphun", 18.578195, 99.017815),
            entry("Uttaradit", 17.625985, 100.096522),
            entry("Chiang Rai", 19.908048, 99.831845),
            entry("Chiang Mai", 18.790219, 98.987038),
            entry("Phrae", 18.145782, 100.141075),
            entry("Mae Hong Son", 19.304090, 97.977189),
            // Northeast
            entry("Kalasin", 16.432537, 103.506057),
            entry("Khon Kaen", 16.438271, 102.838807),
            entry("Chaiyaphum", 15.809822, 102.020389),
            entry("Nakhon Phanom", 17.410651, 104.778428),
            entry("Nakhon Ratchasima", 14.971382, 102.099928),
            entry("Korat", 14.971382, 102.099928),
            entry("Bueng Kan", 18.334008, 103.685012),
            entry("Buri Ram", 14.994507, 103.103858),
            entry("Maha Sarakham", 16.184307, 103.301421),
            entry("Mukdahan", 16.538122, 104.714871),
            entry("Yasothon", 15.794564, 104.140685),
            entry("Roi Et", 16.053088, 103.654758),
            entry("Si Sa Ket", 15.191218, 104.052752),
            entry("Sakon Nakhon", 17.155723, 104.133947),
            entry("Surin", 14.882058, 103.493502),
            entry("Nong Khai", 17.879408, 102.741089),
            entry("Nong Bua Lam Phu", 17.204020, 102.444407),
            entry("Amnat Charoen", 15.858283, 104.629374),
            entry("Udon Thani", 17.415415, 102.785938),
            entry("Ubon Ratchathani", 15.228841, 104.854351),
            entry("Loei", 17.484250, 101.729759),
            // East
            entry("Chanthaburi", 12.610583, 102.104198),
            entry("Chachoengsao", 13.688751, 101.070712),
            entry("Chon Buri", 13.361950, 100.979778),
            entry("Trat", 12.231940, 102.512659),
            entry("Prachin Buri", 14.050551, 101.366699),
            entry("Rayong", 12.668676, 101.274179),
            entry("Sa Kaeo", 13.814727, 102.071741),
            // South
            entry("Krabi", 8.062061, 98.918394),
            entry("Chumphon", 10.492748, 99.179113),
            entry("Trang", 7.557969, 99.609802),
            entry("Nakhon Si Thammarat", 8.419865, 99.962821),
            entry("Narathiwat", 6.428561, 101.822871),
            entry("Pattani", 6.868660, 101.249235),
            entry("Phang Nga", 8.442509, 98.519465),
            entry("Phatthalung", 7.619135, 100.072981),
            entry("Phuket", 7.889262, 98.385376),
            entry("Yala", 6.541759, 101.281588),
            entry("Ranong", 9.967139, 98.633426),
            entry("Songkhla", 7.207486, 100.596251),
            entry("Satun", 6.623390, 100.066649),
            entry("Surat Thani", 9.136008, 99.320337),
            // West
            entry("Kanchanaburi", 14.003961, 99.549867),
            entry("Tak", 16.881891, 99.123635),
            entry("Prachuap Khiri Khan", 11.809087, 99.796383),
            entry("Ratchaburi", 13.528444, 99.812340),
            entry("Phetchaburi", 13.111882, 99.944020),
            // Cities that are not province capitals
            entry("Pattaya", 12.9236, 100.8825),
            entry("Hat Yai", 7.0086, 100.4747));

    private ThaiLocations() {
    }

    /** @return {lat, lon} for a known name, or {@code null} if the name is unknown */
    static double[] find(String name) {
        return COORDINATES.get(normalize(name));
    }

    static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]", "");
    }

    private static Map.Entry<String, double[]> entry(String name, double lat, double lon) {
        return Map.entry(normalize(name), new double[] {lat, lon});
    }
}
