package io.github.advskel.clculator;

import java.util.HashSet;
import java.util.Set;

import org.apfloat.Apcomplex;
import org.apfloat.ApcomplexMath;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;
import org.apfloat.Apint;

/**
 * Represents built-in function definitions in the calculator.
 */
class InternalFunction extends Function {

    /**
     * The string used to represent the argument names in the function. Arguments
     * are
     * numbered <FUNC_ARG>0, <FUNC_ARG>1, etc.
     */
    private static final String FUNC_ARG = "_";

    /**
     * The number of arguments for this function.
     */
    private int numArgs;

    /**
     * The documentation for this function.
     */
    private String docs;

    /**
     * The restriction for this function. This is used to determine if the function
     * can be evaluated with the given arguments.
     */
    private Restriction restriction = Restriction.NONE;

    /**
     * Creates a new internal function with the given name, number of arguments, and
     * documentation.
     * 
     * @param name        The name of the function
     * @param numArgs     The number of arguments for the function
     * @param restriction The restriction for the function
     * @param docs        The documentation for the function
     * @param context     The calculator context
     */
    InternalFunction(String name, int numArgs, Restriction restriction, String docs,
            Calculator context) {
        super(name, null, null, context);
        this.numArgs = numArgs;
        this.docs = docs;
        this.restriction = restriction;
    }

    @Override
    Apcomplex eval(Apcomplex[] args) {
        switch (restriction) {
            case FLOAT -> {
                for (int i = 0; i < numArgs; i++) {
                    if (!args[i].imag().equals(Apfloat.ZERO))
                        throw new IllegalArgumentException("Eval error: function \"" + getName()
                                + "\" requires floating-point argument(s)");
                }
            }
            case INT -> {
                for (int i = 0; i < numArgs; i++) {
                    if (!args[i].isInteger())
                        throw new IllegalArgumentException("Eval error: function \"" + getName()
                                + "\" requires integer argument(s)");
                }
            }
            default -> {
                // do nothing
            }
        }
        switch (getName()) {
            case "pi" -> {
                return ApfloatMath.pi(args[0].longValue());
            }
            case "e" -> {
                return ApfloatMath.e(args[0].longValue());
            }
            case "sin" -> {
                return ApcomplexMath.sin(args[0]);
            }
            case "cos" -> {
                return ApcomplexMath.cos(args[0]);
            }
            case "tan" -> {
                return ApcomplexMath.tan(args[0]);
            }
            case "asin" -> {
                return ApcomplexMath.asin(args[0]);
            }
            case "acos" -> {
                return ApcomplexMath.acos(args[0]);
            }
            case "atan" -> {
                return ApcomplexMath.atan(args[0]);
            }
            case "sqrt" -> {
                return ApcomplexMath.sqrt(args[0]);
            }
            case "exp" -> {
                return ApcomplexMath.exp(args[0]);
            }
            case "abs" -> {
                return ApcomplexMath.abs(args[0]);
            }
            case "gamma" -> {
                return ApcomplexMath.gamma(args[0]);
            }
            case "ln" -> {
                return ApcomplexMath.log(args[0]);
            }
            case "log10" -> {
                return ApcomplexMath.log(new Apfloat(10), args[0]);
            }
            case "log" -> {
                return ApcomplexMath.log(args[1], args[0]);
            }
            case "sinh" -> {
                return ApcomplexMath.sinh(args[0]);
            }
            case "cosh" -> {
                return ApcomplexMath.cosh(args[0]);
            }
            case "tanh" -> {
                return ApcomplexMath.tanh(args[0]);
            }
            case "asinh" -> {
                return ApcomplexMath.asinh(args[0]);
            }
            case "acosh" -> {
                return ApcomplexMath.acosh(args[0]);
            }
            case "atanh" -> {
                return ApcomplexMath.atanh(args[0]);
            }
            case "sinc" -> {
                return ApcomplexMath.sinc(args[0]);
            }
            case "conj" -> {
                return new Apcomplex(args[0].real(), args[0].imag().negate());
            }
            case "re" -> {
                return args[0].real();
            }
            case "im" -> {
                return args[0].imag();
            }
            case "int" -> {
                return args[0].real().truncate();
            }
            case "min" -> {
                Apfloat min = args[0].real();
                for (int i = 1; i < numArgs; i++)
                    min = ApfloatMath.min(min, args[i].real());
                return min;
            }
            case "max" -> {
                Apfloat max = args[0].real();
                for (int i = 1; i < numArgs; i++)
                    max = ApfloatMath.max(max, args[i].real());
                return max;
            }
            case "ceil" -> {
                return ApfloatMath.ceil(args[0].real());
            }
            case "floor" -> {
                return ApfloatMath.floor(args[0].real());
            }
            case "sign" -> {
                if (args[0].real().compareTo(Apfloat.ZERO) > 0)
                    return Apfloat.ONE;
                else if (args[0].real().compareTo(Apfloat.ZERO) < 0)
                    return Apfloat.ONE.negate();
                else
                    return Apfloat.ZERO;
            }
            case "rad" -> {
                return ApfloatMath.toRadians(args[0].real());
            }
            case "deg" -> {
                return ApfloatMath.toDegrees(args[0].real());
            }
            case "rand" -> {
                if (context.precision == -1)
                    return ApfloatMath.random(100);
                return ApfloatMath.random(context.precision);
            }
            case "randint" -> {
                Apfloat rand = ApfloatMath.random(100);
                Apint range = ApfloatMath.abs(args[1].real().subtract(args[0].real())).truncate().add(Apint.ONE);
                if (range.compareTo(Apfloat.ZERO) == 0)
                    return args[0].real();
                Apfloat ans = rand.multiply(range).add(args[0].real());
                return ans.truncate();
            }
            case "choose" -> {
                Apfloat n = args[0].real();
                Apfloat k = args[1].real();
                if (n.compareTo(Apfloat.ZERO) < 0 || k.compareTo(Apfloat.ZERO) < 0)
                    throw new IllegalArgumentException("Eval error: function \"" + getName()
                            + "\" requires non-negative integer argument(s)");
                if (k.compareTo(n) > 0)
                    return Apfloat.ZERO;
                return ApfloatMath.gamma(n.add(Apfloat.ONE))
                        .divide(ApfloatMath.gamma(k.add(Apfloat.ONE))
                                .multiply(ApfloatMath.gamma(n.subtract(k).add(Apfloat.ONE))));
            }
            case "perm" -> {
                Apfloat n = args[0].real();
                Apfloat k = args[1].real();
                if (n.compareTo(Apfloat.ZERO) < 0 || k.compareTo(Apfloat.ZERO) < 0)
                    throw new IllegalArgumentException("Eval error: function \"" + getName()
                            + "\" requires non-negative integer argument(s)");
                if (k.compareTo(n) > 0)
                    return Apfloat.ZERO;
                return ApfloatMath.gamma(n.add(Apfloat.ONE))
                        .divide(ApfloatMath.gamma(n.subtract(k).add(Apfloat.ONE)));
            }
            default -> {
                throw new RuntimeException("INTERNAL ERROR: internal function \"" + getName()
                        + "\" not implemented");
            }
        }

    }

    @Override
    Set<String> references() {
        return new HashSet<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(getName()).append('[');
        for (int i = 0; i < numArgs; i++) {
            sb.append(FUNC_ARG).append(i);
            if (i < numArgs - 1)
                sb.append(", ");
        }
        sb.append(']');
        if (!docs.isEmpty())
            sb.append(": ").append(docs);
        return sb.toString();
    }

    static enum Restriction {
        NONE, FLOAT, INT
    }
}
