public class InterLeave {
    /**
     * @param xlo
     * @param ylo
     * @return long
     * so the goal is we need to leave x and y bits such that
     * y x y x y x .............
     * limits: x and y both are 32 bit max
     *
     * let start with x:
     * assume x =>  1111111.... all bits are ones
     *
     * so what we do first create space of 16 bits, followed by 8, 4, 2, 1
     * now how to create a space of 16 bit ?.
     * 1: left shift bits by 16 let's call it x'
     * 2: z = x | x' =>  after this what will happens there are 16 overlapping bits
     * 3: z = z & (mask of alternate 0s and 1s in a pair of 16 => this will clear overlapping bits
     * result 32 bits broke into 2 pair which are 16 bits apart
     *
     * repeat same activity with left shift of 8
     * z = z & (mask of alternate 0s and 1s in a pair of 8
     * => 4 pair which are 8 bits apart
     *
     * repeat same activity with left shift of 4
     * z = z & (mask of alternate 0s and 1s in a pair of 4
     * => 8 pair which are 4 bits apart
     *
     * repeat same activity with left shift of 2
     * z = z & (mask of alternate 0s and 1s in a pair of 2
     * => 16 pair which are 2 bits apart
     *
     * repeat same activity with left shift of 1
     * z = z & (mask of alternate 0s and 1s in a pair of 1
     * => 32 pair which are 1 bits apart
     *
     * Now do the same thing for y
     *
     * after both x and y look something like this
     * x = 1010101010101....
     * y = 1010101010101....
     * now for interleaving shift left by 1
     *
     * x =       101010101...
     * y =      1010101010...
     *
     * interleaving
     * x | y => 11111111111
     *
     */
    public static long interleave64(int xlo, int ylo) {
        final long[] B = {
                0x5555555555555555L,
                0x3333333333333333L,
                0x0F0F0F0F0F0F0F0FL,
                0x00FF00FF00FF00FFL,
                0x0000FFFF0000FFFFL
        };
        final int[] S = {1, 2, 4, 8, 16};

        long x = Integer.toUnsignedLong(xlo);
        long y = Integer.toUnsignedLong(ylo);

        // Log initial values of x and y in binary
        System.out.println("Initial x: " + toBinaryString(x));
//            System.out.println("Initial y: " + toBinaryString(y));

        for (int i = 4; i >= 0; i--) {
            // Log shift operation
            long xShifted = x << S[i];
            long yShifted = y << S[i];
            System.out.println("x << S[" + i + "] (" + S[i] + "): " + toBinaryString(xShifted));
//                System.out.println("y << S[" + i + "] (" + S[i] + "): " + toBinaryString(yShifted));

            // Log OR operation
            long xOr = x | xShifted;
            long yOr = y | yShifted;
            System.out.println("x | (x << S[" + i + "]): " + toBinaryString(xOr));
//                System.out.println("y | (y << S[" + i + "]): " + toBinaryString(yOr));

            // Log AND operation
            long xAnd = xOr & B[i];
            long yAnd = yOr & B[i];
            System.out.println("B[" + i + "]: " + toBinaryString(B[i]));
            System.out.println("x & B[" + i + "]: " + toBinaryString(xAnd));
            System.out.println("y & B[" + i + "]: " + toBinaryString(yAnd));

            // Update x and y with the new values
            x = xAnd;
            y = yAnd;
        }

        // Final result
        y = (y << 1);
        System.out.println("y: " + toBinaryString(y));
        long result = x | (y);
        System.out.println("result: " + toBinaryString(result));

//            System.out.println("Final result (x | (y << 1)): " + toBinaryString(result));

        return result;
    }

    // Helper method to convert long to binary string with leading zeros
    private static String toBinaryString(long value) {
        return String.format("%64s", Long.toBinaryString(value)).replace(' ', '0');
    }

    public static void main(String[] args) {
        long ans = interleave64(Integer.MAX_VALUE, Integer.MAX_VALUE);
        System.out.println(ans);

    }
}