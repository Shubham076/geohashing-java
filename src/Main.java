import java.util.*;

public class Main {

    private static final  double MERCATOR_MAX = 20037726.37;

    private static final long[] B = {
            0x5555555555555555L,
            0x3333333333333333L,
            0x0F0F0F0F0F0F0F0FL,
            0x00FF00FF00FF00FFL,
            0x0000FFFF0000FFFFL
    };

    private static final long[] B_REV = {
            0x5555555555555555L,
            0x3333333333333333L,
            0x0F0F0F0F0F0F0F0FL,
            0x00FF00FF00FF00FFL,
            0x0000FFFF0000FFFFL,
            0x00000000FFFFFFFFL
    };

    private static final int[] S = {1, 2, 4, 8, 16};
    private static final int[] S_REV = {0, 1, 2, 4, 8, 16};

    public static long interleave64(long lon, long lat) {
        long xlon = lon;
        long ylat = lat;

        for (int i = S.length - 1; i >= 0; i--) {
            ylat = (ylat | (ylat << S[i])) & B[i];
            xlon = (xlon | (xlon << S[i])) & B[i];
        }
//        [Long, Lat]
        return (xlon << 1) | ylat;
    }

    public static class GeoHashRange {
        double min, max;
    }

    // Mercator projection works well for latitudes between about -85.0511° and +85.0511°
    public static final double GEO_LAT_MIN = -85.05112878;
    public static final double GEO_LAT_MAX = 85.05112878;
    public static final double GEO_LONG_MIN = -180.0;
    public static final double GEO_LONG_MAX = 180.0;
    public static final int STEPS_MAX = 26;

    public static int getBits(int r) {
        if (r == 0) return STEPS_MAX;
        int bits = 1;
        while (r < MERCATOR_MAX) {
            r *= 2;
            bits += 1;  // one for long and 1 for lat
        }
        bits -= 2;
        if (bits > 26) return 26;
        if (bits < 0) return 1;
        return bits;
    }

    public static void geohashGetCoordRange(GeoHashRange longRange, GeoHashRange latRange) {
        longRange.min = GEO_LONG_MIN;
        longRange.max = GEO_LONG_MAX;
        latRange.min = GEO_LAT_MIN;
        latRange.max = GEO_LAT_MAX;
    }

    public static long encode(GeoHashRange longRange, GeoHashRange latRange,
                                 double longitude, double latitude, int bits) {
        if (bits > 26 || bits == 0 ||
                (latRange.max - latRange.min == 0) ||
                (longRange.max - longRange.min == 0)) return 0;

        if (longitude < GEO_LONG_MIN || longitude > GEO_LONG_MAX ||
                latitude < GEO_LAT_MIN || latitude > GEO_LAT_MAX) return 0;

        if (latitude < latRange.min || latitude > latRange.max ||
                longitude < longRange.min || longitude > longRange.max) {
            return 0;
        }

        // convert to value [0, 1] 0.5 =  middle, > 0.5 means upper and < 0.5 means lower
        double latOffset = (latitude - latRange.min) / (latRange.max - latRange.min);
        double longOffset = (longitude - longRange.min) / (longRange.max - longRange.min);

        longOffset *= (1L << bits);
        latOffset *= (1L << bits);

        long latFixed = (long) latOffset;
        long longFixed = (long) longOffset;

        long res = interleave64(longFixed, latFixed);
        return res;
    }

    public static ArrayList<Double> decode(GeoHashRange longRange, GeoHashRange latRange, long no, int bits) {
        long ylat = no;
        long xlon = no >> 1;

        for (int i = 0; i < S_REV.length; i++) {
            ylat = (ylat | (ylat >> S_REV[i])) & B_REV[i];
            xlon = (xlon | (xlon >> S_REV[i])) & B_REV[i];
        }

//        System.out.println(Long.toBinaryString(ylat));
//        System.out.println(Long.toBinaryString(xlon));

        double latScale = latRange.max - latRange.min;
        double longScale = longRange.max - longRange.min;

        double lonMin = longRange.min +  xlon * 1.0 / (1L << bits) * longScale;
        double lonMax = longRange.min +  (xlon + 1) * 1.0 / (1L << bits) * longScale;
        double latMin = latRange.min +  ylat * 1.0 / (1L << bits) * latScale;
        double latMax = latRange.min +  (ylat + 1) * 1.0 / (1L << bits) * latScale;

        double lat = (latMax + latMin) / 2.0;
        double lon = (lonMax + lonMin) / 2.0;
        return new ArrayList<Double>(Arrays.asList(lon, lat));
    }

    public static String getGeoHash(long no) {
        String s = "0123456789bcdefghjkmnpqrstuvwxyz";
        StringBuilder hash = new StringBuilder();
        GeoHashRange longRange = new GeoHashRange();
        GeoHashRange latRange = new GeoHashRange();
        geohashGetCoordRange(longRange, latRange);
        List<Double> coords = decode(longRange, latRange, no, STEPS_MAX);
        latRange.min = -90;
        latRange.max = 90;
        long newNo = encode(longRange, latRange, coords.get(0), coords.get(1), STEPS_MAX);

        int i = 0;
        while (hash.length() < 11) {
            long idx = 0;
            /* We have just 52 bits, but the API used to output
             * an 11 bytes geohash. For compatibility we assume
             * zero. */
            if (i == 10) {
                idx = 0;
            } else {
                idx = (newNo >> (52-((i+1)*5))) & 0x1F;
            }
            hash.append(s.charAt((int) idx));
            i += 1;
        }
        return hash.toString();
    }

    public static SortedSet<Long> getNearByHashes(TreeSet<Long> zset, double lon, double lat, int km) {
        int steps = getBits(km * 1000); // in meters
        GeoHashRange longRange = new GeoHashRange();
        GeoHashRange latRange = new GeoHashRange();
        geohashGetCoordRange(longRange, latRange);

        // we got the 52 bit
        long hash = encode(longRange, latRange, lon, lat, steps);
        System.out.println("Hash calculated for coord: " + hash + " " + getGeoHash(hash));
        // adjust to remove trailing zeroes, adjust according to kms
        long minHash = hash << (STEPS_MAX * 2) - (steps * 2);
        long maxHash = (hash + 1) << (STEPS_MAX * 2) - (steps * 2);

        System.out.println("Min hash: " + minHash + " " +  getGeoHash(minHash));
        System.out.println("Max hash: " + maxHash + " " + getGeoHash(maxHash));

        // calculate the min value and the max value and then search in zset to get the coordinates
        return zset.subSet(minHash, maxHash);
    }

    static int getBitFromPrecision(int len) {
        return ((len * 5) + 1) / 2;
    }

    public static void main(String[] args) {
        TreeSet<Long> set = new TreeSet<>();
        double [][] coords = {
                {48.669, -4.329},
                {2.3522, 48.891},
                {77.02863369033248, 28.60370082035767},
        };
        int bits = STEPS_MAX; // Adjust precision (max 26 for 52-bit geohash total)
        GeoHashRange longRange = new GeoHashRange();
        GeoHashRange latRange = new GeoHashRange();
        geohashGetCoordRange(longRange, latRange);

        for (double[] coord: coords) {
            long hashValue = encode(longRange, latRange, coord[0], coord[1], bits);
            set.add(hashValue);
        }
        System.out.println("--------------------");

        for (Long value : set) {
            System.out.println(value + " " + getGeoHash(value));
            System.out.println(decode(longRange, latRange, value, bits));
        }

//        List<Double> coords = decode(longRange, latRange, hashValue, step);
//        System.out.println(coords);

        System.out.println("\n--------Nearest Neighbour results------------");
        SortedSet<Long> results = getNearByHashes(set, 76.99258479950257, 28.606714986233914, 10000);
        for (Long value: results) {
            System.out.println(value + " " +  getGeoHash(value) + " " + decode(longRange, latRange, value, bits));
        }
        System.out.println("Done");
    }
}