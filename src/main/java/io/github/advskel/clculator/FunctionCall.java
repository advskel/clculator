package io.github.advskel.clculator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apfloat.Apcomplex;
import org.apfloat.Apint;

/**
 * Represents a function call (like `f[x,y]`) in an expression.
 */
class FunctionCall extends Operand {
    /*
     * The function name (like `f` in `f[x,y]`)
     */
    private String func;

    /*
     * The arguments of the function call (like [x, y] in `f[x,y]`)
     */
    private List<Operand> args;

    /**
     * The references of the function call (like `x` and `y` in `f[x,y]`).
     */
    private Set<String> references;

    /**
     * Creates a new function call with the given name and arguments.
     *
     * @param name    The name of the function.
     * @param args    The arguments of the function call.
     * @param context The calculator context.
     */
    private FunctionCall(String name, List<Operand> args, Calculator context) {
        super(context);
        func = name;
        this.args = args;
        references = new HashSet<>();
        for (Operand arg : args)
            references.addAll(arg.references());
        references.add(func);
    }

    @Override
    Apcomplex eval(Apcomplex[] args) {
        if (func.equals("sum") || func.equals("prod")) {
            if (this.args.size() != 4)
                throw new IllegalArgumentException("Eval error: function \""
                        + func + "\" must have exactly four arguments");
            boolean isSum = func.equals("sum");
            String counter = ((Variable) this.args.get(0)).toString();
            if (context.globalVars.containsKey(counter) ||
                    context.globalFuncs.containsKey(counter) ||
                    Calculator.RESERVED_COMMANDS.contains(counter))
                throw new IllegalArgumentException("Eval error: " + func
                        + " counter name \"" + counter + "\" is already in use");

            Apcomplex test = this.args.get(1).eval(null);
            if (!test.isInteger())
                throw new IllegalArgumentException("Eval error: " + func
                        + " start bound \"" + this.args.get(1)
                        + "\" is not an integer");
            Apint start = test.real().truncate();

            test = this.args.get(2).eval(null);
            if (!test.isInteger())
                throw new IllegalArgumentException("Eval error: " + func
                        + " end bound \"" + this.args.get(2)
                        + "\" is not an integer");
            Apint end = test.real().truncate();

            if (start.compareTo(end) > 0)
                throw new IllegalArgumentException("Eval error: " + func
                        + " start bound \"" + start + "\" is greater than end bound \""
                        + end + "\"");

            Apcomplex ans = Apcomplex.ZERO;
            Apcomplex backup = null;
            if (context.globalVars.containsKey(counter))
                backup = context.globalVars.get(counter);
            while (start.compareTo(end) <= 0) {
                context.globalVars.put(counter, start);
                if (isSum)
                    ans = ans.add(this.args.get(3).eval(null));
                else
                    ans = ans.multiply(this.args.get(3).eval(null));
                start = start.add(Apint.ONE);
            }
            if (backup != null)
                context.globalVars.put(counter, backup);
            else
                context.globalVars.remove(counter);
            return ans;
        }
        if (!context.globalFuncs.containsKey(func))
            throw new IllegalArgumentException("Eval error: function \"" + func
                    + "\" is not defined");

        Function f = context.globalFuncs.get(func);
        Apcomplex[] computedArgs = new Apcomplex[this.args.size()];
        for (int i = 0; i < this.args.size(); i++)
            computedArgs[i] = this.args.get(i).eval(null);

        return f.eval(computedArgs);
    }

    @Override
    Set<String> references() {
        return references;
    }

    /**
     * Compiles a string into a function call.
     * 
     * @param s       the string to compile
     * @param context the calculator context
     * @return a FunctionCall object representing the value of the string
     * @throws IllegalArgumentException if the string is not a valid function call
     */
    static FunctionCall compile(String s, Calculator context) {
        if (!COMPILED_PATTERN.matcher(s).find())
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid function call");

        // extract function name
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        String funcName = s.substring(0, start);

        // extract arguments to the function call, which can be separated by a comma
        // the function call might be nested, and we need to find the outermost
        // function call arguments
        // e.g., f[f[x+1, 2], f[y, 3]] has arguments f[x+1, 2] and f[y, 3].
        String args = s.substring(start + 1, end);
        if (args.isEmpty())
            return new FunctionCall(funcName, new ArrayList<>(), context);

        // stores the positions of any commas, so given [-1, 3, 10, 20], the arguments
        // are substring(0, 3), substring(4, 10), and substring(11, 20). we add -1
        // and args.length() to the list to make it the extraction code more concise
        // later
        ArrayList<Integer> argIndexes = new ArrayList<>();
        argIndexes.add(-1);
        int depth = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '[')
                depth++;
            else if (c == ']')
                depth--;
            else if (c == ',' && depth == 0)
                argIndexes.add(i);
        }
        argIndexes.add(args.length());

        // now extract the substrings (to get the arguments themselves) and compile them
        ArrayList<Operand> argsList = new ArrayList<>();
        for (int i = 0; i < argIndexes.size() - 1; i++) {
            String arg = args.substring(argIndexes.get(i) + 1, argIndexes.get(i + 1));
            if (arg.isEmpty())
                throw new IllegalArgumentException("Compile error: \"" + s
                        + "\" is not a valid function call");
            argsList.add(Operand.compile(arg, context));
        }

        if (funcName.equals("sum") || funcName.equals("prod")) {
            if (argsList.size() != 4)
                throw new IllegalArgumentException("Compile error: function \""
                        + funcName + "\" must have exactly four arguments");
            if (!(argsList.get(0) instanceof Variable))
                throw new IllegalArgumentException("Compile error: function \""
                        + funcName + "\" first argument must be a variable");
        }

        return new FunctionCall(funcName, argsList, context);
    }

    @Override
    public String toString() {
        return func + args;
    }
}
