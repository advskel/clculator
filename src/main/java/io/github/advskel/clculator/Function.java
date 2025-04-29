package io.github.advskel.clculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apfloat.Apcomplex;

/**
 * Function class that represents a user-defined function in the calculator.
 */
class Function extends Operand {
    /**
     * The name of the function.
     */
    private String name;

    /**
     * The arguments of the function.
     */
    private List<String> args;

    /**
     * The body of the function.
     */
    private Operand body;

    /**
     * The (base) cases of the function.
     */
    private FunctionCache baseCases;

    /**
     * The cache for the function. Resets when any variable in the function's body
     * is
     * changed.
     */
    private FunctionCache cache;

    /**
     * The set of variables/functions that are referenced in the function.
     */
    private Set<String> references;

    /**
     * The string regex for a function definition.
     * <p>
     * A function definition is of the form: <br>
     * <code>name[args] = body</code> <br>
     * where <code>name</code> is the name of the function, <code>args</code> are
     * the variable arguments, and <code>body</code> is the body of the function.
     */
    static final String DEFINITION;

    /**
     * The string regex for a function case.
     * <p>
     * A function case is of the form: <br>
     * <code>name[args] = body</code> <br>
     * where <code>name</code> is the name of the function, <code>args</code> are
     * the numeric arguments, and <code>body</code> is the value of the function.
     */
    static final String CASE;

    /**
     * The compiled pattern for a function definition.
     */
    static final Pattern COMPILED_DEFINITION;

    /**
     * The compiled pattern for a function case.
     */
    static final Pattern COMPILED_CASE;

    static {
        DEFINITION = "^" + Variable.PATTERN + "\\[(?:" + Variable.PATTERN + "(?:,"
                + Variable.PATTERN + ")*)?\\]=.+";
        COMPILED_DEFINITION = Pattern.compile(DEFINITION);

        CASE = "^" + Variable.PATTERN + "\\[" + Numeral.PATTERN + "(?:,"
                + Numeral.PATTERN + ")*\\]=" + Numeral.PATTERN + "$";
        COMPILED_CASE = Pattern.compile(CASE);
    }

    /**
     * Creates a new function with the given name, arguments, and body.
     * 
     * @param name    The name of the function
     * @param args    The arguments of the function
     * @param body    The body of the function
     * @param context The calculator context
     */
    Function(String name, List<String> args, Operand body, Calculator context) {
        super(context);
        this.name = name;
        this.args = args;
        this.body = body;

        if (body != null) {
            for (String arg : args) {
                if (context.globalVars.containsKey(arg) || context.globalFuncs.containsKey(arg)
                        || Calculator.RESERVED_COMMANDS.contains(arg)) {
                    throw new IllegalArgumentException("Compile error: \"" + arg
                            + "\" is a reserved keyword and cannot be used as a function argument");
                }
            }
            this.baseCases = new FunctionCache();
            this.cache = new FunctionCache();
            this.references = new HashSet<>();
            references.addAll(body.references());
            for (String arg : args)
                references.remove(arg);
        }
    }

    @Override
    Apcomplex eval(Apcomplex[] args) {
        if (args.length != this.args.size())
            throw new IllegalArgumentException("Eval error: invalid function call to \""
                    + getName() + "\": expected " + this.args.size()
                    + " argument(s) but found " + args.length + " instead");

        // check if the function has any base cases
        Apcomplex result = baseCases.get(args);
        if (result != null)
            return result;

        // check (for recursive functions) cached values
        result = cache.get(args);
        if (result != null)
            return result;

        // create a new map for the function arguments
        Map<String, Apcomplex> overridden = null;
        for (int i = 0; i < args.length; i++) {
            if (context.globalVars.containsKey(this.args.get(i))) {
                if (overridden == null)
                    overridden = new HashMap<>();
                overridden.put(this.args.get(i), context.globalVars.get(this.args.get(i)));
            }
            context.globalVars.put(this.args.get(i), args[i]);
        }

        // evaluate the body of the function
        result = body.eval(null);

        // add the result to the cache
        cache.add(args, result);

        // restore the original variables
        for (int i = 0; i < args.length; i++) {
            if (overridden != null && overridden.containsKey(this.args.get(i))) {
                context.globalVars.put(this.args.get(i), overridden.get(this.args.get(i)));
            } else {
                context.globalVars.remove(this.args.get(i));
            }
        }

        return result;
    }

    @Override
    Set<String> references() {
        return references;
    }

    /**
     * Resets the cache for the function.
     * <p>
     * This is used when a variable/function in the function's body is changed.
     */
    void resetCache() {
        cache = new FunctionCache();
    }

    /**
     * Compiles a function definition string into a Function object.
     * 
     * @param s       The function definition string
     * @param context The calculator context
     * @return A Function object representing the compiled function
     * @throws IllegalArgumentException if the function definition is invalid or
     *                                  redefines a reserved keyword
     */
    static Function compile(String s, Calculator context) {
        if (!COMPILED_DEFINITION.matcher(s).find())
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid function definition");

        // the function definition compilation is much simpler than the function call
        // compilation because function definition is not recursive
        int start = s.indexOf('[');
        int end = s.indexOf(']');
        String name = s.substring(0, start);

        if (Calculator.RESERVED_FUNCS.contains(name) || Calculator.RESERVED_COMMANDS.contains(name))
            throw new IllegalArgumentException("Compile error: \"" + name
                    + "\" is a reserved keyword and cannot be redefined");

        List<String> args = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(s.substring(start + 1, end), ",");
        while (st.hasMoreTokens())
            args.add(st.nextToken());

        Operand body = Operand.compile(s.substring(end + 2), context);
        return new Function(name, args, body, context);
    }

    /**
     * Adds a base case to the function.
     * 
     * @param s       The function case definition expression
     * @param context The calculator context
     * @return The modified Function object with the added case
     */
    static Function addBaseCase(String s, Calculator context) {
        if (!COMPILED_CASE.matcher(s).find())
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid function case");

        int start = s.indexOf('[');
        int end = s.indexOf(']');
        String name = s.substring(0, start);

        if (!context.globalFuncs.containsKey(name))
            throw new IllegalArgumentException("Compile error: \"" + name
                    + "\" is not a defined function");
        if (Calculator.RESERVED_FUNCS.contains(name) || Calculator.RESERVED_COMMANDS.contains(name))
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is a reserved keyword and cannot be redefined");

        Function f = context.globalFuncs.get(name);
        List<Numeral> args = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(s.substring(start + 1, end), ",");
        while (st.hasMoreTokens())
            args.add(Numeral.compile(st.nextToken(), context));

        Apcomplex body = Numeral.compile(s.substring(end + 2), context).eval(null);
        Apcomplex[] argArray = new Apcomplex[args.size()];
        for (int i = 0; i < args.size(); i++)
            argArray[i] = args.get(i).eval(null);

        f.baseCases.add(argArray, body);

        return f;
    }

    /**
     * Returns the name of the function.
     * 
     * @return The name of the function
     */
    String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder def = new StringBuilder();
        def.append(name).append(args).append(" := ").append(body);
        StringTokenizer st = new StringTokenizer(baseCases.toString(), "\n");
        while (st.hasMoreTokens()) {
            def.append("\n  ").append(name).append(st.nextToken());
        }
        return def.toString();
    }
}
