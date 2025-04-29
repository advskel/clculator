package io.github.advskel.clculator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apfloat.Apcomplex;
import org.apfloat.ApfloatMath;

/**
 * Calculator class that provides a simple command-line calculator.
 */
public class Calculator {
    /**
     * Reserved command names
     */
    static final Set<String> RESERVED_COMMANDS;

    /**
     * Reserved variable names
     */
    static final Set<String> RESERVED_VARS;

    /**
     * Reserved function names
     */
    static final Set<String> RESERVED_FUNCS;

    /**
     * Defined global variables
     */
    final Map<String, Apcomplex> globalVars;

    /**
     * Defined global functions
     */
    final Map<String, Function> globalFuncs;

    /**
     * The precision of the calculator. This is the number of digits to which
     * calculations are performed.
     */
    long precision = 32;

    static {
        // Reserve variable, function, and command names
        RESERVED_VARS = new HashSet<>();
        RESERVED_FUNCS = new HashSet<>();
        RESERVED_COMMANDS = new HashSet<>();

        RESERVED_COMMANDS.add("reset");
        RESERVED_COMMANDS.add("exit");
        RESERVED_COMMANDS.add("help");
        RESERVED_COMMANDS.add("env");
        RESERVED_COMMANDS.add("del");
        RESERVED_COMMANDS.add("precision");

        RESERVED_VARS.add("i");
        RESERVED_VARS.add("pi");
        RESERVED_VARS.add("e");
        RESERVED_VARS.add("ans");

        RESERVED_FUNCS.add("sum");
        RESERVED_FUNCS.add("prod");
        RESERVED_FUNCS.add("sin");
        RESERVED_FUNCS.add("cos");
        RESERVED_FUNCS.add("tan");
        RESERVED_FUNCS.add("asin");
        RESERVED_FUNCS.add("acos");
        RESERVED_FUNCS.add("atan");
        RESERVED_FUNCS.add("sqrt");
        RESERVED_FUNCS.add("exp");
        RESERVED_FUNCS.add("abs");
        RESERVED_FUNCS.add("min");
        RESERVED_FUNCS.add("max");
        RESERVED_FUNCS.add("sign");
        RESERVED_FUNCS.add("rad");
        RESERVED_FUNCS.add("deg");
        RESERVED_FUNCS.add("log");
        RESERVED_FUNCS.add("ln");
        RESERVED_FUNCS.add("log10");
        RESERVED_FUNCS.add("gamma");
        RESERVED_FUNCS.add("sinh");
        RESERVED_FUNCS.add("cosh");
        RESERVED_FUNCS.add("tanh");
        RESERVED_FUNCS.add("ceil");
        RESERVED_FUNCS.add("floor");
        RESERVED_FUNCS.add("sinc");
        RESERVED_FUNCS.add("asinh");
        RESERVED_FUNCS.add("acosh");
        RESERVED_FUNCS.add("atanh");
        RESERVED_FUNCS.add("pi");
        RESERVED_FUNCS.add("e");
        RESERVED_FUNCS.add("conj");
        RESERVED_FUNCS.add("re");
        RESERVED_FUNCS.add("im");
        RESERVED_FUNCS.add("int");
        RESERVED_FUNCS.add("rand");
        RESERVED_FUNCS.add("randint");
        RESERVED_FUNCS.add("choose");
        RESERVED_FUNCS.add("perm");
    }

    /**
     * Creates a new calculator instance. This instance has no user-defined
     * variables or functions.
     */
    public Calculator() {
        globalVars = new TreeMap<>();
        globalFuncs = new TreeMap<>();
        setConstants();
        setFuncs();

        // sanity check to ensure all reserved variables and functions are defined
        for (String var : RESERVED_VARS)
            if (!globalVars.containsKey(var))
                throw new RuntimeException("INTERNAL ERROR: reserved variable " + var
                        + " not defined!");
        for (String func : RESERVED_FUNCS)
            if (!globalFuncs.containsKey(func))
                throw new RuntimeException("INTERNAL ERROR: reserved function " + func
                        + " not defined!");
        for (String var : globalVars.keySet())
            if (!RESERVED_VARS.contains(var))
                throw new RuntimeException("INTERNAL ERROR: defined variable " + var
                        + " not reserved!");
        for (String func : globalFuncs.keySet())
            if (!RESERVED_FUNCS.contains(func))
                throw new RuntimeException("INTERNAL ERROR: defined function " + func
                        + " not reserved!");
    }

    /**
     * Executes a command in the calculator. The command can be an expression, a
     * variable assignment, a function definition, or a command.
     * 
     * @param s The command to execute
     * @return The result of the command. `null` represents an exit command.
     */
    public String execute(String s) {
        // exit the program
        if (s == null || s.equals("exit")) {
            System.out.println("Goodbye!\n");
            return null;
        }

        // remove a defined variable or function
        if (s.startsWith("del")) {
            if (s.length() < 5)
                return "Invalid command. Usage: del <name>";
            String name = s.substring(4).trim();
            if (globalVars.containsKey(name)) {
                if (RESERVED_VARS.contains(name))
                    return "Cannot delete reserved variable \"" + name + "\"";
                globalVars.remove(name);
                return "Deleted variable \"" + name + "\"";
            } else if (globalFuncs.containsKey(name)) {
                if (RESERVED_FUNCS.contains(name))
                    return "Cannot delete reserved function \"" + name + "\"";
                globalFuncs.remove(name);
                return "Deleted function \"" + name + "\"";
            } else {
                return "Variable or function \"" + name + "\" not found.";
            }
        }

        // set the precision of the calculator
        if (s.startsWith("precision")) {
            String sub = s.substring(9).trim();
            if (sub.equals("auto")) {
                precision = -1;
                setConstants();
                resetFunctionCaches(null);
                return "Precision set to auto (significant figures).";
            }
            try {
                precision = Long.parseLong(sub);
                if (precision < 1)
                    throw new NumberFormatException();
                setConstants();
                resetFunctionCaches(null);
                return "Precision set to " + precision + " digit(s).";
            } catch (NumberFormatException e) {
                return "Invalid precision value. Must be a positive number.";
            }
        }

        // remove all whitespace
        s = s.replaceAll("\\s+", "");

        // if the string is empty, return
        if (s.isEmpty())
            return "";

        // reset all custom-defined variables and functions
        if (s.equals("reset")) {
            resetContext();
            return "All variables and functions have been reset.";
        }

        // display help text
        if (s.equals("help")) {
            System.out.println("Available commands:");
            System.out.println("  reset - resets all user-defined variables and functions");
            System.out.println("  exit - exits the calculator");
            System.out.println("  help - displays this help text");
            System.out.println("  env - displays all defined variables and functions");
            System.out.println("  del <name> - deletes a variable or function");
            System.out.println("  precision <number> - sets the precision of the calculator");
            System.out.println("  precision auto - sets the precision to auto (significant figures)");

            System.out.println("\nTo solve an expression, type it in directly like `3 + 2 * (5 - x)`.");
            System.out.println("Call functions like `f[x]` or `sin[pi/2]` or `min[2,3]`.");
            System.out.println("Note that whitespace is stripped, so `1 2 3` will be interpreted as `123`.");

            System.out.println("\nTo define a global variable, do `name = expression` like `x = 5 + 4`.");

            System.out.println("\nTo define a function, do `name[arg1,arg2,...] = expression`");
            System.out.println("like `f[x] = x^2` or `g[]=3`. Function arguments override global variables,");
            System.out.println("so `f[x] = x + 1` will use the value of x passed to f, not the global x.");
            System.out.println("It is therefore recommended to name function arguments like `_x` to avoid confusion.");

            System.out.println(
                    "\nTo define a base case for a (recursive) function, use the form `name[arg1,arg2,...] = value`");
            System.out.println("like `f[0] = 1` or `g[0,0] = 2`. The values must all be numerals.");
            System.out.println("To increase the recursion depth, add Java VM argument `-Xss<size>` like `-Xss20m`.");
            return "\nTo view this help text again, input `help`.";
        }

        // display all defined variables and functions
        if (s.equals("env")) {
            if (precision == -1)
                System.out.println("Global precision: auto");
            else
                System.out.println("Global precision: " + precision + " digits");

            System.out.println("Internal variables: ");
            for (String var : globalVars.keySet()) {
                if (!RESERVED_VARS.contains(var))
                    continue;
                Numeral n = new Numeral(globalVars.get(var), this);
                System.out.println("  " + var + " = " + n.toString());
            }
            System.out.println("\nInternal functions: ");
            for (String func : globalFuncs.keySet()) {
                if (!RESERVED_FUNCS.contains(func))
                    continue;
                System.out.println("  " + globalFuncs.get(func));
            }
            System.out.println("\nUser-defined variables: ");
            for (String var : globalVars.keySet()) {
                if (RESERVED_VARS.contains(var))
                    continue;
                Numeral n = new Numeral(globalVars.get(var), this);
                System.out.println("  " + var + " = " + n.toString());
            }
            System.out.println("\nUser-defined functions: ");
            for (String func : globalFuncs.keySet()) {
                if (RESERVED_FUNCS.contains(func))
                    continue;
                StringTokenizer st = new StringTokenizer(globalFuncs.get(func).toString(), "\n");
                System.out.println("  " + st.nextToken());
                while (st.hasMoreTokens()) {
                    System.out.println("  " + st.nextToken());
                }
            }
            return "";
        }

        // if a function definition, define the function & compile the expression
        if (Function.COMPILED_DEFINITION.matcher(s).find()) {
            Function f = Function.compile(s, this);
            if (globalFuncs.containsKey(f.getName()))
                resetFunctionCaches(f.getName());
            globalFuncs.put(f.getName(), f);
            return f.toString();
        }

        // if a function case definition, add the case to the function
        if (Function.COMPILED_CASE.matcher(s).find()) {
            Function f = Function.addBaseCase(s, this);
            resetFunctionCaches(f.getName());
            return f.toString();
        }

        // if a variable definition, define the variable & compile the expression
        if (Variable.COMPILED_DEFINITION.matcher(s).find()) {
            int sign = s.indexOf('=');
            String name = s.substring(0, sign);
            if (Calculator.RESERVED_VARS.contains(name) || Calculator.RESERVED_COMMANDS.contains(name))
                throw new IllegalArgumentException("Compile error: \"" + name
                        + "\" is a reserved keyword and cannot be reassigned.");
            Operand e = Operand.compile(s.substring(sign + 1), this);
            Apcomplex result = e.eval(null);
            if (globalVars.containsKey(name))
                resetFunctionCaches(name);
            globalVars.put("ans", result);
            globalVars.put(name, result);
            resetFunctionCaches("ans");
            return name + " := " + new Numeral(result, this).toString();
        }

        // otherwise, compile the expression and evaluate it
        Operand e = Operand.compile(s, this);
        Apcomplex result = e.eval(null);
        globalVars.put("ans", result);
        resetFunctionCaches("ans");
        return new Numeral(result, this).toString();
    }

    /**
     * Resets the context of the calculator. This method clears all user-defined
     * variables and functions and reinitializes the reserved variables and
     * functions.
     */
    public void resetContext() {
        for (String func : globalFuncs.keySet())
            if (!RESERVED_FUNCS.contains(func))
                globalFuncs.remove(func);
        for (String var : globalVars.keySet())
            if (!RESERVED_VARS.contains(var))
                globalVars.remove(var);
    }

    /**
     * Adds the reserved variables to the global variable map.
     */
    private void setConstants() {
        globalVars.put("i", Apcomplex.I);
        if (precision == -1) {
            globalVars.put("pi", ApfloatMath.pi(100));
            globalVars.put("e", ApfloatMath.e(100));
        } else {
            globalVars.put("pi", ApfloatMath.pi(precision + 1));
            globalVars.put("e", ApfloatMath.e(precision + 1));
        }
        if (!globalVars.containsKey("ans"))
            globalVars.put("ans", Apcomplex.ZERO);
    }

    /**
     * Adds the reserved functions to the global function map.
     */
    private void setFuncs() {
        globalFuncs.put("sin", new InternalFunction("sin", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("cos", new InternalFunction("cos", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("tan", new InternalFunction("tan", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("asin", new InternalFunction("asin", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("acos", new InternalFunction("acos", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("atan", new InternalFunction("atan", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("sqrt", new InternalFunction("sqrt", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("exp", new InternalFunction("exp", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("abs", new InternalFunction("abs", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("min", new InternalFunction("min", 2,
                InternalFunction.Restriction.INT, "", this));
        globalFuncs.put("max", new InternalFunction("max", 2,
                InternalFunction.Restriction.INT, "", this));
        globalFuncs.put("sign", new InternalFunction("sign", 1,
                InternalFunction.Restriction.FLOAT,
                "-1 if negative, 1 if positive, 0 if zero", this));
        globalFuncs.put("gamma", new InternalFunction("gamma", 1,
                InternalFunction.Restriction.FLOAT,
                "to calculate n! for positive int n, do gamma[n+1]", this));
        globalFuncs.put("rad", new InternalFunction("rad", 1,
                InternalFunction.Restriction.FLOAT,
                "convert degrees to radians", this));
        globalFuncs.put("deg", new InternalFunction("deg", 1,
                InternalFunction.Restriction.FLOAT,
                "convert radians to degrees", this));
        globalFuncs.put("ln", new InternalFunction("ln", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("log10", new InternalFunction("log10", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("log", new InternalFunction("log", 2,
                InternalFunction.Restriction.FLOAT, "log[base, x]", this));
        globalFuncs.put("sinh", new InternalFunction("sinh", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("cosh", new InternalFunction("cosh", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("tanh", new InternalFunction("tanh", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("ceil", new InternalFunction("ceil", 1,
                InternalFunction.Restriction.FLOAT, "", this));
        globalFuncs.put("floor", new InternalFunction("floor", 1,
                InternalFunction.Restriction.FLOAT, "", this));
        globalFuncs.put("sinc", new InternalFunction("sinc", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("asinh", new InternalFunction("asinh", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("acosh", new InternalFunction("acosh", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("atanh", new InternalFunction("atanh", 1,
                InternalFunction.Restriction.NONE, "", this));
        globalFuncs.put("pi", new InternalFunction("pi", 1,
                InternalFunction.Restriction.INT,
                "calculates pi to number of digits", this));
        globalFuncs.put("e", new InternalFunction("e", 1,
                InternalFunction.Restriction.INT,
                "calculates e to number of digits", this));
        globalFuncs.put("conj", new InternalFunction("conj", 1,
                InternalFunction.Restriction.NONE, "complex conjugate", this));
        globalFuncs.put("re", new InternalFunction("re", 1,
                InternalFunction.Restriction.NONE, "real part", this));
        globalFuncs.put("im", new InternalFunction("im", 1,
                InternalFunction.Restriction.NONE, "imaginary part", this));
        globalFuncs.put("int", new InternalFunction("int", 1,
                InternalFunction.Restriction.NONE, "truncate decimal", this));
        globalFuncs.put("rand", new InternalFunction("rand", 0,
                InternalFunction.Restriction.NONE,
                "random number between 0 and 1", this));
        globalFuncs.put("randint", new InternalFunction("randint", 2,
                InternalFunction.Restriction.INT,
                "random integer between a and b inclusive", this));
        globalFuncs.put("choose", new InternalFunction("choose", 2,
                InternalFunction.Restriction.INT,
                "n choose k", this));
        globalFuncs.put("perm", new InternalFunction("perm", 2,
                InternalFunction.Restriction.INT,
                "n permute k", this));
        globalFuncs.put("sum", new InternalFunction("sum", 4,
                InternalFunction.Restriction.NONE,
                "summation of `_3` for `_0` = `_1` to `_2` inclusive, e.g., sum[x, 1, 10, x^2]", this));
        globalFuncs.put("prod", new InternalFunction("prod", 4,
                InternalFunction.Restriction.NONE,
                "product of `_3` for `_0` = `_1` to `_2` inclusive, e.g., prod[x, 1, 10, x^2]", this));
    }

    /**
     * Resets the caches of all functions that reference a changed
     * variable/function.
     * 
     * @param name The name of the variable. If null, all function caches are reset.
     */
    private void resetFunctionCaches(String name) {
        for (Function f : globalFuncs.values())
            if (name == null || f.references().contains(name))
                f.resetCache();
    }

    /**
     * Main method to run the calculator in command line.
     * 
     * @param args (unused) command line arguments
     * @throws Exception if an error occurs while reading input
     */
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Calculator calculator = new Calculator();
        System.out.println("Welcome to Adam's command-line calculator! Enter 'help' to view commands and usage.\n");
        while (true) {
            System.out.print("> ");
            String input = br.readLine();
            try {
                String result = calculator.execute(input);
                if (result == null)
                    break;
                System.out.println(result);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } catch (StackOverflowError e) {
                System.out.println("Eval error: function recursion too deep");
            }
            System.out.println();
        }
        br.close();
    }
}
