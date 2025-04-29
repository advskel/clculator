package io.github.advskel.clculator;

/**
 * A symbol is a token that does not evaluate to a value, like an operator or a
 * parenthesis.
 */
abstract class Symbol extends Token {
    private String symbol;

    /**
     * Create a new symbol
     * 
     * @param token   the string representation of the symbol
     * @param context the calculator context
     */
    Symbol(String token, Calculator context) {
        super(context);
        symbol = token;
    }

    @Override
    public String toString() {
        return symbol;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Symbol other = (Symbol) obj;
        return symbol.equals(other.symbol);
    }
}
