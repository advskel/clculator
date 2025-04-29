package io.github.advskel.clculator;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apfloat.Apcomplex;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

/**
 * Represents a numeral in the expression. A numeral can be an integer, floating
 * point,
 * or in scientific notation. It can also be positive or negative.
 */
class Numeral extends Operand {
    /**
     * The value of the numeral.
     */
    private Apcomplex n;

    /**
     * A regex pattern that matches a numeral. The pattern matches:
     * <ul>
     * <li>an optional sign (+ or -)</li>
     * <li>a sequence of digits</li>
     * <li>an optional decimal point followed by a sequence of digits</li>
     * <li>an optional exponent (e or E followed by an optional sign and a sequence
     * of digits)</li>
     * </ul>
     */
    static final String PATTERN = "\\d+\\.?\\d*(?:[eE][+-]?\\d+)?i?";

    /**
     * A compiled regex pattern that matches a numeral.
     */
    static final Pattern COMPILED_PATTERN = Pattern.compile(PATTERN);

    static Apfloat LOWER_BOUND = new Apfloat("0.0001");

    static Apfloat UPPER_BOUND = new Apfloat("1000000");

    /**
     * Constructs a Numeral object with the given value.
     *
     * @param n the value of the numeral
     * @param context the calculator context
     */
    Numeral(Apcomplex n, Calculator context) {
        super(context);
        if (n instanceof Apfloat f) {
            this.n = new Apcomplex(f, Apfloat.ZERO);
        } else {
            this.n = n;
        }
    }

    @Override
    public Apcomplex eval(Apcomplex[] args) {
        return n;
    }

    @Override
    Set<String> references() {
        return new HashSet<>();
    }

    /**
     * Compiles a string into a Numeral object. The string must match the pattern
     * defined
     * in {@link #PATTERN}.
     *
     * @param s the string to compile
     * @param context the calculator context
     * @return a Numeral object representing the value of the string
     * @throws IllegalArgumentException if the string is not a valid numeral
     */
    static Numeral compile(String s, Calculator context) {
        if (!COMPILED_PATTERN.matcher(s).find())
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid numeral");

        boolean imag = false;
        if (s.endsWith("i")) {
            s = s.substring(0, s.length() - 1);
            imag = true;
        }
        Apfloat part;
        if (context.precision == -1)
            part = new Apfloat(s);
        else
            part = new Apfloat(s, context.precision);

        if (imag)
            return new Numeral(new Apcomplex(Apfloat.ZERO, part), context);
        else
            return new Numeral(new Apcomplex(part, Apfloat.ZERO), context);
    }

    @Override
    public String toString() {
        // real part only
        if (n.imag().equals(Apfloat.ZERO))
            return floatToString(n.real(), context);

        // imaginary part only
        if (n.real().equals(Apfloat.ZERO)) {
            if (n.imag().compareTo(Apfloat.ONE) == 0)
                return "i";
            return floatToString(n.imag(), context) + "i";
        }

        // both real and imaginary parts
        String realPart = floatToString(n.real(), context);
        String imaginaryPart;
        if (n.imag().compareTo(Apfloat.ONE) == 0)
            imaginaryPart = "i";
        else if (n.imag().compareTo(Apfloat.ONE.negate()) == 0)
            imaginaryPart = "-i";
        else
            imaginaryPart = floatToString(n.imag(), context) + "i";
        return realPart + (n.imag().compareTo(Apfloat.ZERO) >= 0 ? "+" : "") + imaginaryPart;
    }

    /**
     * Converts a floating point number to a string. If the number is between
     * {@link #LOWER_BOUND} and {@link #UPPER_BOUND}, it is converted to a raw
     * numeral
     * string. Otherwise, it is converted to a scientific notation string.
     *
     * @param f the floating point number to convert
     * @param context the calculator context
     * @return the string representation of the number
     */
    private static String floatToString(Apfloat f, Calculator context) {
        if (context.precision == -1 || LOWER_BOUND.equals(Apfloat.ZERO))
            return f.toString(false);

        Apfloat abs = ApfloatMath.abs(f);
        if (abs.compareTo(Apfloat.ZERO) == 0 ||
                abs.compareTo(LOWER_BOUND) >= 0 && abs.compareTo(UPPER_BOUND) < 0)
            return f.toString(true);
        return f.toString(false);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Numeral other = (Numeral) obj;
        return n.equals(other.n);
    }
}
