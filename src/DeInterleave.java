public class DeInterleave {

    public static long deinterleave64(long interleaved) {
        final long[] B = {
                0x5555555555555555L,
                0x3333333333333333L,
                0x0F0F0F0F0F0F0F0FL,
                0x00FF00FF00FF00FFL,
                0x0000FFFF0000FFFFL,
                0x00000000FFFFFFFFL
        };
        final int[] S = {0, 1, 2, 4, 8, 16};

        long x = interleaved;
        long y = interleaved >> 1;

        System.out.println("Initial interleaved: " + toBinaryString(interleaved));
        System.out.println("Initial x: " + toBinaryString(x));
        System.out.println("Initial y (interleaved >>> 1): " + toBinaryString(y));

        for (int i = 0; i < S.length; i++) {
            long xShifted = x >> S[i];
            long yShifted = y >> S[i];

            System.out.println("Step " + i + ": S[" + i + "] = " + S[i]);

            System.out.println("x >> S[" + i + "]: " + toBinaryString(xShifted));
            System.out.println("y >> S[" + i + "]: " + toBinaryString(yShifted));

            long xOr = x | xShifted;
            long yOr = y | yShifted;

            System.out.println("x | (x >> S[" + i + "]): " + toBinaryString(xOr));
            System.out.println("y | (y >> S[" + i + "]): " + toBinaryString(yOr));

            long xAnd = xOr & B[i];
            long yAnd = yOr & B[i];

            System.out.println("B[" + i + "]: " + toBinaryString(B[i]));
            System.out.println("x & B[" + i + "]: " + toBinaryString(xAnd));
            System.out.println("y & B[" + i + "]: " + toBinaryString(yAnd));

            x = xAnd;
            y = yAnd;
        }

        long result = x | (y << 32);
        System.out.println("Final x: " + toBinaryString(x));
        System.out.println("Final y << 32: " + toBinaryString(y << 32));
        System.out.println("Final result: " + toBinaryString(result));

        return result;
    }

    private static String toBinaryString(long value) {
        return String.format("%64s", Long.toBinaryString(value)).replace(' ', '0');
    }

    public static void main(String[] args) {
        // Example usage
        long interleaved = 4611686018427387903L;
        long result = deinterleave64(interleaved);
        System.out.println("DeInterleaved result: " + result);
    }
}

