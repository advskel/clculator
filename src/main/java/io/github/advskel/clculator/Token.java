package io.github.advskel.clculator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A token represents a part of an expression. It can be a variable, numeral,
 * binary operator, grouping symbol, or function call. For lazy evaluation
 * purposes, an
 * expression (a combination of the above tokens) is itself also considered a
 * token.
 */
abstract class Token {
    /**
     * A regex pattern that matches a valid operand. The pattern matches:
     * <ul>
     * <li>any valid function call</li>
     * <li>any valid variable</li>
     * <li>any valid numeral</li>
     * <li>any valid binary operator</li>
     * <li>any valid grouping symbol</li>
     * </ul>
     */
    static final String PATTERN;

    /**
     * A compiled regex pattern that matches a valid operand.
     */
    static final Pattern COMPILED_PATTERN;

    /**
     * This string is used as a placeholder in the compilation process to indicate
     * where a function call is in the expression. The string is a control character
     * that is not used in any other part of the expression.
     */
    static final String FUNC_CTRL = " ";

    /**
     * A regex string that matches the beginning of a function call: any valid
     * function name followed by an opening bracket.
     */
    private static final String FUNC_CALL = Variable.PATTERN + "\\[";

    /**
     * A compiled regex pattern that matches the beginning of a function call.
     */
    private static final Pattern COMPILED_FUNC_CALL = Pattern.compile(FUNC_CALL);

    /**
     * The calculator context that this token is associated with. This is used to
     * evaluate the token.
     */
    protected final Calculator context;

    /**
     * Creates a new token with the given calculator context.
     *
     * @param context the calculator context
     */
    Token(Calculator context) {
        this.context = context;
    }

    static {
        String valid = "(" + FUNC_CTRL + ")|("
                + Numeral.PATTERN + ")|("
                + Variable.PATTERN + ")|("
                + BinaryOperator.PATTERN + ")|("
                + Grouper.PATTERN + ")";
        String invalid = "((?:(?!" + valid + ").)+)";

        PATTERN = valid + "|" + invalid;
        COMPILED_PATTERN = Pattern.compile(PATTERN);
    }

    /**
     * Compiles a string into a token/expression. The string must match the pattern
     * defined in {@link #PATTERN}. The string can be a valid function call,
     * variable,
     * numeral, binary operator, grouper, or expression (a combination of these
     * tokens).
     *
     * @param s the string to compile
     * @param context the calculator context
     * @return a Token object representing the value of the string
     * @throws IllegalArgumentException if the string is not a valid token
     */
    static Token compile(String s, Calculator context) {
        /*
         * NOTE: it is possible to make the compiler a lot better by:
         * - performing a single scan of the string instead of multiple regex scans
         * - using a stack to keep track of the function calls and their arguments
         * instead of using recursion
         * - generating an abstract syntax tree (AST) instead of a list of tokens and
         * using operational semantics to evaluate expressions
         * - using a more performant language like C++ or Rust
         * 
         * But that is beyond the scope of this project and would require a lot of
         * effort to implement properly.
         */

        // Check if the string contains the function placeholder character
        if (s.contains(FUNC_CTRL))
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid calculator expression");

        // Check if the string contains any function calls
        // Function calls cannot be scanned via regex because they can be recursively
        // nested like `f[f[x+1, 2], f[y, 3]]. Instead, we use a regex to find the
        // function calls and then manually parse the string to find the outer function
        // calls.
        Matcher funcFinder = COMPILED_FUNC_CALL.matcher(s);

        // Contains the list of substring groups that are not function calls
        // e.g., [0, 5, 11, 20] represents substring(0, 5) and substring(11, 20)
        ArrayList<Integer> nonFuncGroups = new ArrayList<>();
        nonFuncGroups.add(0);

        // Contains the function call strings found in the expression
        Queue<String> funcs = new LinkedList<>();
        int current = -1; // the current scan position
        while (funcFinder.find()) {
            // find function call starts (like `func[`) and check if it is nested or not
            int start = funcFinder.start();
            if (start < current)
                continue;
            current = funcFinder.end();

            // Check the "depth" of the function calls
            // e.g., f[f[x+1, 2], f[y, 3]] has a depth of 2
            // when depth is 0, we have ended the outermost call, which is what we are
            // trying to extract
            int depth = 1;
            while (current < s.length()) {
                char c = s.charAt(current++);
                if (c == '[')
                    depth++;
                else if (c == ']')
                    depth--;
                if (depth == 0)
                    break;
            }
            if (depth != 0)
                throw new IllegalArgumentException("Compile error: \"" + s
                        + "\" missing closing function bracket ] at position " + current);

            funcs.add(s.substring(funcFinder.start(), current));
            nonFuncGroups.add(funcFinder.start());
            nonFuncGroups.add(current);
        }
        nonFuncGroups.add(s.length());

        // if there exist function calls, we temporarily replace them in the original
        // expression with the function control character so we can do regex processing
        // on the entire expression without worrying about nested function calls
        if (!funcs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nonFuncGroups.size() - 1; i += 2) {
                sb.append(s, nonFuncGroups.get(i), nonFuncGroups.get(i + 1));
                if (i < nonFuncGroups.size() - 2)
                    sb.append(FUNC_CTRL);
            }
            s = sb.toString();
        }

        // do regex processing on the entire expression to extract variables, numerals,
        // binary operators, and grouping symbols (and function calls)
        Matcher tokenizer = COMPILED_PATTERN.matcher(s);
        ArrayList<String> tokens = new ArrayList<>();
        while (tokenizer.find())
            tokens.add(tokenizer.group());

        if (tokens.isEmpty())
            throw new IllegalArgumentException("Compile error: \"" + s
                    + "\" is not a valid calculator expression");

        if (tokens.size() == 1) {
            Token t = compileToken(tokens.get(0), funcs, context);
            if (!(t instanceof Operand e))
                throw new IllegalArgumentException("Compile error: \""
                        + tokens.get(0) + "\" is not a valid calculator expression");
            return e;
        }

        ArrayList<Token> compiled = new ArrayList<>();
        for (String token : tokens)
            compiled.add(compileToken(token, funcs, context));
        Expression e = new Expression(compiled, context);
        return e;
    }

    /**
     * Compiles a non-expression string. The string must match the pattern defined
     * in
     * {@link #PATTERN}. The string can be a valid function call, variable, numeral,
     * binary operator, or grouper.
     *
     * @param token the string to compile
     * @param funcs the functions found in the expression
     * @param context the calculator context
     * @return a Token object representing the value of the string
     * @throws IllegalArgumentException if the string is not a valid token
     */
    private static Token compileToken(String token, Queue<String> funcs, 
    Calculator context) {
        if (token.equals(FUNC_CTRL))
            return FunctionCall.compile(funcs.poll(), context);

        if (Numeral.COMPILED_PATTERN.matcher(token).find())
            return Numeral.compile(token, context);

        if (Variable.COMPILED_PATTERN.matcher(token).find())
            return Variable.compile(token, context);

        if (BinaryOperator.COMPILED_PATTERN.matcher(token).find())
            return BinaryOperator.compile(token, context);

        if (Grouper.COMPILED_PATTERN.matcher(token).find())
            return Grouper.compile(token, context);

        throw new IllegalArgumentException("Compile error: \"" + token
                + "\" is not a valid token");
    }
}
