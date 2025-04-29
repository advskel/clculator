package io.github.advskel.clculator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A grouping symbol, like parentheses or brackets.
 */
class Grouper extends Symbol {

    /*
     * The regex string for identifying grouping symbols: ( and )
     */
    static final String PATTERN = "[()]";

    /*
     * The compiled regex pattern for identifying grouping symbols.
     */
    static final Pattern COMPILED_PATTERN = Pattern.compile(PATTERN);

    /*
     * The map of opening and closing pairs of grouping symbols.
     */
    private static final Map<String, String> PAIRS;

    /*
     * The set of opening grouping symbols.
     */
    private static final Set<String> OPENING;

    static {
        PAIRS = new HashMap<>();
        PAIRS.put("(", ")");
        PAIRS.put(")", "(");
        
        OPENING = new HashSet<>();
        OPENING.add("(");
    }

    /*
     * Creates a new Grouper object.
     * @param s the string representation of the grouping symbol
     * @param context the calculator context
     */
    private Grouper(String s, Calculator context) {
        super(s, context);
    }

    /**
     * Checks if this grouping symbol is an opening symbol like "(".
     * @return true if this is an opening symbol, false otherwise.
     */
    boolean isOpening() {
        return OPENING.contains(toString());
    }

    /**
     * Checks if a given grouping symbol matches this one. For example, "(" matches
     * ")" and vice versa.
     * @return true if this and `other` match, false otherwise.
     */
    boolean matches(Grouper other) {
        return PAIRS.get(toString()).equals(other.toString());
    }

    /**
     * Compiles a string into a grouping symbol.
     * @param s the string to compile.
     * @param context the calculator context.
     * @return the compiled Grouper object.
     * @throws IllegalArgumentException if the string is not a valid grouping symbol.
     */
    static Grouper compile(String s, Calculator context) {
        if (!COMPILED_PATTERN.matcher(s).find())
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid grouping symbol.");

        return new Grouper(s, context);
    }
    
}
