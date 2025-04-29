package io.github.advskel.clculator;

import java.util.regex.Pattern;

import org.apfloat.Apcomplex;
import org.apfloat.ApcomplexMath;
import org.apfloat.Apfloat;
import org.apfloat.Apint;

/**
 * BinaryOperator represents a binary operator in an expression. A binary
 * operator takes a value before and a value after it, and applies a function to
 * them.
 */
class BinaryOperator extends Symbol {

    /*
     * The regex string for identifying a binary operator: * + - / % ^
     */
    static final String PATTERN = "[*^+/%-]";

    /*
     * The compiled regex pattern for identifying a binary operator
     */
    static final Pattern COMPILED_PATTERN = Pattern.compile(PATTERN);

    /*
     * Create a new BinaryOperator with the given string representation.
     */
    private BinaryOperator(String s, Calculator context) {
        super(s, context);
    }

    /**
     * a.precedes(b) returns, given an expression x <a> y <b> z, whether x <a> y
     * should be computed before y <b> z.
     * 
     * @param other The <other> operator to compare
     * @return Whether this has precedence over `other`.
     */
    boolean precedes(BinaryOperator other) {
        String a = toString();
        String b = other.toString();

        if (a.equals("^") || b.equals("^"))
            return a.equals("^") && !b.equals("^");

        return (a.equals("*") || a.equals("/") || a.equals("%")) && !(b.equals("*") || b.equals("/") || b.equals("%"));
    }

    /**
     * a.compute(x, y) calculates x <a> y.
     * 
     * @param first  First operand
     * @param second Second operand
     * @return The result of the binary operator applied to the operands
     */
    Apcomplex compute(Apcomplex first, Apcomplex second) {
        switch (toString()) {
        case "*" -> {
            return first.multiply(second);
        }
        case "^" -> {
            if (second.isInteger())
                return pow(first, second.real().truncate());
            return ApcomplexMath.pow(first, second);
        }
        case "+" -> {
            return first.add(second);
        }
        case "/" -> {
            return first.divide(second);
        }
        case "%" -> {
            if (first.imag().equals(Apfloat.ZERO) && second.imag().equals(Apfloat.ZERO)) {
                // if both numbers are real, use the real modulus
                Apfloat ans = first.real().mod(second.real());
                return new Apcomplex(ans, Apfloat.ZERO);
            }
            throw new IllegalArgumentException("Eval error: cannot apply remainder operator to complex numbers.");
        }
        case "-" -> {
            return first.subtract(second);
        }
        default -> {
            throw new IllegalStateException("INTERNAL ERROR: operator " + toString() + " not implemented");
        }
        }
    }

    /**
     * Compile a string into a BinaryOperator. The string must be a valid operator
     * (see {@link #PATTERN}).
     * 
     * @param s       The string to compile
     * @param context The calculator context
     * @return The compiled BinaryOperator
     */
    static BinaryOperator compile(String s, Calculator context) {
        if (!COMPILED_PATTERN.matcher(s).find())
            throw new IllegalArgumentException("Compile error: \"" + s + "\" is not a valid binary operator.");

        return new BinaryOperator(s, context);
    }

    private static final Apint TWO = Apint.ONE.add(Apint.ONE);

    /**
     * Computes an integer power. Preferable to using ApcomplexMath.pow() for
     * integer powers, as it is more precise.
     * 
     * @param first  The base
     * @param second The exponent
     * @return The result of raising the base to the exponent
     */
    private static Apcomplex pow(Apcomplex first, Apint second) {
        if (second.equals(Apint.ZERO)) {
            return Apcomplex.ONE;
        } else if (second.equals(Apint.ONE)) {
            return first;
        } else if (second.equals(Apint.ONE.negate())) {
            return Apcomplex.ONE.divide(first);
        }

        Apcomplex half = pow(first, second.divide(TWO));
        Apcomplex result = half.multiply(half);
        if (second.mod(TWO).equals(Apint.ONE))
            result = result.multiply(first);

        return result;
    }
}
