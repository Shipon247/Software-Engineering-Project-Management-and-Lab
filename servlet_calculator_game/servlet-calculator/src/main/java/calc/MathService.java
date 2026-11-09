package calc;

/**
 * All the maths used by the game. Every answer in the game is calculated by one
 * of these methods - nothing is stored or hard-coded.
 */
public class MathService {

    /** How close a decimal guess must be to the real answer (e.g. 0.87 vs 0.8660). */
    public static final double TOLERANCE = 0.01;

    public static final int PRIME_MARKS = 2;
    public static final int MOD_MARKS = 2;
    public static final int TRIG_MARKS = 3;

    /** Returns 1 if the number is prime, otherwise 0. Numbers below 2 are not prime. */
    public static int isPrime(int number) {
        if (number < 2) return 0;
        if (number < 4) return 1;          // 2 and 3
        if (number % 2 == 0) return 0;
        for (int i = 3; (long) i * i <= number; i += 2) {
            if (number % i == 0) return 0;
        }
        return 1;
    }

    /** Returns the remainder of a / b. The divisor must not be zero. */
    public static int calculateMod(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Divisor must not be zero");
        }
        return a % b;
    }

    /** Sine of an angle given in degrees. */
    public static double calculateSin(double degree) {
        return Math.sin(Math.toRadians(degree));
    }

    /** Cosine of an angle given in degrees. */
    public static double calculateCos(double degree) {
        return Math.cos(Math.toRadians(degree));
    }

    /** Tangent of an angle given in degrees. Undefined at 90, 270, ... degrees. */
    public static double calculateTan(double degree) {
        if (!isTanDefined(degree)) {
            throw new ArithmeticException("Tangent is undefined at " + degree + " degrees");
        }
        return Math.tan(Math.toRadians(degree));
    }

    public static boolean isTanDefined(double degree) {
        double d = ((degree % 180) + 180) % 180;
        return Math.abs(d - 90) > 1e-9;
    }

    /** fn is "sin", "cos" or "tan". */
    public static double calculateTrig(String fn, double degree) {
        switch (fn) {
            case "sin": return calculateSin(degree);
            case "cos": return calculateCos(degree);
            case "tan": return calculateTan(degree);
            default: throw new IllegalArgumentException("Unknown function: " + fn);
        }
    }

    public static boolean isClose(double guess, double answer) {
        return Math.abs(guess - answer) <= TOLERANCE;
    }

    /** Scoring logic from the assignment. Maximum is 10. */
    public static int totalScore(boolean primeCorrect, boolean modCorrect,
                                 boolean trig1Correct, boolean trig2Correct) {
        int totalScore = 0;
        if (primeCorrect) totalScore += PRIME_MARKS;
        if (modCorrect)   totalScore += MOD_MARKS;
        if (trig1Correct) totalScore += TRIG_MARKS;
        if (trig2Correct) totalScore += TRIG_MARKS;
        return totalScore;
    }
}
