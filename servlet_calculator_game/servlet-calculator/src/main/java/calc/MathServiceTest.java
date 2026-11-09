package calc;

/**
 * Simple tests (no extra libraries needed). Run this class's main() method.
 * Every line should print PASS.
 */
public class MathServiceTest {
    private static int failed = 0;

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failed++;
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    public static void main(String[] args) {
        // Prime
        check("isPrime(7) = 1", MathService.isPrime(7) == 1);
        check("isPrime(8) = 0", MathService.isPrime(8) == 0);
        check("isPrime(1) = 0", MathService.isPrime(1) == 0);
        check("isPrime(0) = 0", MathService.isPrime(0) == 0);
        check("isPrime(-5) = 0", MathService.isPrime(-5) == 0);
        check("isPrime(2) = 1", MathService.isPrime(2) == 1);
        check("isPrime(97) = 1", MathService.isPrime(97) == 1);
        check("isPrime(100) = 0", MathService.isPrime(100) == 0);

        // Modulus
        check("17 % 5 = 2", MathService.calculateMod(17, 5) == 2);
        check("-17 % 5 = -2", MathService.calculateMod(-17, 5) == -2);
        check("17 % -5 = 2", MathService.calculateMod(17, -5) == 2);
        check("0 % 5 = 0", MathService.calculateMod(0, 5) == 0);
        try {
            MathService.calculateMod(17, 0);
            check("divisor 0 gives validation error", false);
        } catch (IllegalArgumentException e) {
            check("divisor 0 gives validation error", true);
        }

        // Trigonometry
        check("sin(30) = 0.5", near(MathService.calculateSin(30), 0.5));
        check("cos(60) = 0.5", near(MathService.calculateCos(60), 0.5));
        check("tan(45) = 1.0", near(MathService.calculateTan(45), 1.0));
        try {
            MathService.calculateTan(90);
            check("tan(90) is undefined", false);
        } catch (ArithmeticException e) {
            check("tan(90) is undefined", true);
        }

        // Scoring
        check("all correct = 10", MathService.totalScore(true, true, true, true) == 10);
        check("all wrong = 0", MathService.totalScore(false, false, false, false) == 0);
        check("only prime right = 2", MathService.totalScore(true, false, false, false) == 2);
        check("only trig right = 6", MathService.totalScore(false, false, true, true) == 6);

        System.out.println(failed == 0 ? "\nAll tests passed." : "\n" + failed + " test(s) FAILED.");
    }
}
