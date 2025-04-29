package io.github.advskel.clculator;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apfloat.Apcomplex;

/**
 * Represents a variable in the calculator like x, y, z, etc.
 */
class Variable extends Operand {
    private String name;

    /**
     * The regex string for a valid variable name. A variable name must start with a
     * letter or underscore, followed by any number of letters, digits, or
     * underscores.
     */
    static final String PATTERN = "[A-Za-z_]\\w*";

    /**
     * The compiled regex pattern for a valid variable name.
     */
    static final Pattern COMPILED_PATTERN = Pattern.compile(PATTERN);

    /**
     * The regex string for a valid variable definition. A variable definition must
     * be in the form of "name = value", where name is a valid variable name and
     * value is any expression.
     */
    static final String DEFINITION = "^" + PATTERN + "=.+";

    /**
     * The compiled regex pattern for a valid variable definition.
     */
    static final Pattern COMPILED_DEFINITION = Pattern.compile(DEFINITION);

    /**
     * Creates a new variable with the given name and negation status.
     *
     * @param token   the name of the variable
     * @param neg     whether the variable is negated
     * @param context the calculator context
     */
    private Variable(String token, Calculator context) {
        super(context);
        name = token;
    }

    @Override
    Apcomplex eval(Apcomplex[] args) {
        if (!context.globalVars.containsKey(name))
            throw new IllegalArgumentException("Eval error: \"" + name
                    + "\" is not defined as a variable/command (functions require brackets [])");
        return context.globalVars.get(name);
    }

    @Override
    Set<String> references() {
        HashSet<String> refs = new HashSet<>();
        refs.add(name);
        return refs;
    }

    /**
     * Compiles a string into a Variable object. The string must be a valid variable
     * name.
     *
     * @param s       the string to compile
     * @param context the calculator context
     * @return the compiled Variable object
     * @throws IllegalArgumentException if the string is not a valid variable name
     *                                  or
     *                                  if the string is a reserved keyword
     */
    static Variable compile(String s, Calculator context) {
        if (!COMPILED_PATTERN.matcher(s).find())
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid variable");

        return new Variable(s, context);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof Variable other))
            return false;
        return name.equals(other.name);
    }
}
