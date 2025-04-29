package io.github.advskel.clculator;

import java.util.Set;

import org.apfloat.Apcomplex;

/**
 * An operand is anything (numeral, variable, expression) that can be evaluated
 * to a number.
 */
abstract class Operand extends Token {

    /**
     * Creates a new operand with the given calculator context.
     *
     * @param context the calculator context
     */
    Operand(Calculator context) {
        super(context);
    }

    /**
     * Evaluates the operand using the given variables and functions. The variables
     * and functions are passed as maps, where the keys are the names of the
     * variables/functions and the values are their corresponding values.
     * 
     * @param args  the arguments to the operand (only used for function calls)
     * @return the evaluated result of the operand
     */
    abstract Apcomplex eval(Apcomplex[] args);

    /**
     * Returns the set of variables/functions referenced by the operand.
     * 
     * @return a Set of names that the operand references
     */
    abstract Set<String> references();

    /**
     * Compiles a string into an operand. The string can be a valid function call,
     * variable, numeral, or expression.
     *
     * @param s the string to compile
     * @param context the calculator context
     * @return an Operand object representing the value of the string
     * @throws IllegalArgumentException if the string is not a valid operand
     */
    static Operand compile(String s, Calculator context) {
        Token t = Token.compile(s, context);

        if (t instanceof Operand)
            return (Operand) t;
        else
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid evaluable operand/expression");
    }
}
