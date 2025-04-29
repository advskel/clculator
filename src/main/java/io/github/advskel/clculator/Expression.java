package io.github.advskel.clculator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apfloat.Apcomplex;

/**
 * An Expression is a sequence of tokens that can be evaluated to a single
 * value.
 */
class Expression extends Operand {

    private List<Token> expression;

    /*
     * Constructs an Expression from a list of tokens.
     * @param expression the list of tokens that make up the expression
     * @param context    the calculator context
     */
    Expression(List<Token> expression, Calculator context) {
        super(context);
        this.expression = expression;
    }

    /*
     * Evaluates the expression with Dijkstra's two-stack algorithm.
     * 
     * @param vars a map of variable names to their values
     * 
     * @param funcs a map of function names to their definitions
     * 
     * @return the result of the evaluation
     */
    @Override
    Apcomplex eval(Apcomplex[] args) {
        Stack<Operand> values = new Stack<>();
        Stack<Symbol> symbols = new Stack<>();

        // expectingOperand is true if the next token should be an operand
        boolean expectingOperand = true;

        for (Token token : expression) {
            if (token instanceof Operand o) {
                // o is a number or variable or anything that can be evaluated
                // push it onto the stack (will evaluate it when popping)
                if (!expectingOperand) {
                    throw new IllegalStateException("Eval error: unexpected operand \""
                            + token + "\".");
                }
                values.push(o);
                expectingOperand = false;
            } else if (token instanceof Grouper g) {
                // g is an opening grouping symbol like (
                // push it onto the stack
                if (g.isOpening()) {
                    if (!expectingOperand) {
                        throw new IllegalStateException("Eval error: unexpected opening symbol \""
                                + token + "\".");
                    }
                    symbols.push(g);
                    continue;
                }

                // g is a closing grouping symbol like )
                // solve expressions until we find the matching opening symbol
                if (expectingOperand) {
                    throw new IllegalStateException("Eval error: unexpected closing symbol \""
                            + token + "\".");
                }
                if (symbols.isEmpty()) {
                    throw new IllegalStateException("Eval error: unmatched closing symbol \"" + token
                            + "\".");
                }

                // to solve, pop operator, pop two operands, compute, push resulting
                // numeral back into operands stack
                boolean completed = false;
                while (!symbols.isEmpty()) {
                    Symbol top = symbols.pop();
                    if (top instanceof Grouper g2 && g2.matches(g)) {
                        completed = true;
                        break;
                    }

                    BinaryOperator op = (BinaryOperator) top;
                    if (values.size() < 2) {
                        throw new IllegalStateException("Eval error: not enough operands for operator \""
                                + op + "\".");
                    }
                    Operand right = values.pop();
                    Operand left = values.pop();
                    Apcomplex leftValue = left.eval(null);
                    Apcomplex rightValue = right.eval(null);
                    Apcomplex result = op.compute(leftValue, rightValue);
                    values.push(new Numeral(result, context));
                }

                if (!completed) {
                    throw new IllegalStateException("Eval error: unmatched opening symbol for \""
                            + token + "\".");
                }
            } else if (token instanceof BinaryOperator op) {
                // op is an operator like +, -, *, /, etc.
                if (expectingOperand) {
                    // unary - or + operator (like -x or +x)
                    if (op.toString().equals("-")) {
                        values.push(new Numeral(Apcomplex.ZERO, context));
                        symbols.push(op);
                        continue;
                    } else if (op.toString().equals("+"))
                        continue;
                    
                    throw new IllegalStateException("Eval error: unexpected operator \""
                            + token + "\".");
                }
                expectingOperand = true;
                if (symbols.isEmpty() || symbols.peek() instanceof Grouper
                        || op.precedes((BinaryOperator) symbols.peek())) {
                    // if op has higher precedence than the top of the stack, push it
                    symbols.push(op);
                    continue;
                }

                // otherwise, solve one operation at a time until op has higher precedence
                // then push it
                while (!symbols.isEmpty()) {
                    Symbol top = symbols.peek();
                    if (top instanceof Grouper || top instanceof BinaryOperator other && op.precedes(other))
                        break;

                    symbols.pop();
                    if (values.size() < 2)
                        throw new IllegalStateException("Eval error: not enough operands for operator \""
                                + top + "\".");

                    Operand right = values.pop();
                    Operand left = values.pop();
                    Apcomplex leftValue = left.eval(null);
                    Apcomplex rightValue = right.eval(null);
                    Apcomplex result = ((BinaryOperator) top).compute(leftValue, rightValue);
                    values.push(new Numeral(result, context));
                }
                symbols.push(op);
            } else {
                throw new IllegalStateException("Eval error: unknown token \""
                        + token + "\".");
            }
        }

        // finished parsing the expression, now solve the remaining operations
        while (!symbols.isEmpty()) {
            Symbol top = symbols.pop();
            if (top instanceof Grouper) {
                throw new IllegalStateException("Eval error: unmatched grouping symbol \""
                        + top + "\".");
            }
            if (values.size() < 2) {
                throw new IllegalStateException("Eval error: not enough operands for operator \""
                        + top + "\".");
            }
            Operand right = values.pop();
            Operand left = values.pop();
            Apcomplex leftValue = left.eval(null);
            Apcomplex rightValue = right.eval(null);
            Apcomplex result = ((BinaryOperator) top).compute(leftValue, rightValue);
            values.push(new Numeral(result, context));
        }
        if (values.size() != 1) {
            throw new IllegalStateException("Eval error: expression \"" + this
                    + "\" does not evaluate to a single value.");
        }
        return values.pop().eval(null);
    }

    @Override
    Set<String> references() {
        Set<String> refs = new HashSet<>();
        for (Token token : expression)
            if (token instanceof Operand o)
                refs.addAll(o.references());
            
        return refs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Token e : expression)
            sb.append(e.toString());
        return sb.toString();
    }

}
