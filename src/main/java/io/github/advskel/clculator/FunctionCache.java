package io.github.advskel.clculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apfloat.Apcomplex;

/**
 * Stores the function cache for a function. The cache is a tree structure where each
 * node represents an argument of the function. The value of the leaf node is the
 * result of the function for the chain of arguments leading to that node.
 */
class FunctionCache {
    private Map<Apcomplex, FunctionCache> next = null;
    private Apcomplex value = null;

    FunctionCache() {}

    void add(Apcomplex[] args, Apcomplex value) {
        add(args, 0, value);
    }

    private void add(Apcomplex[] args, int index, Apcomplex value) {
        if (args == null || index == args.length) {
            this.value = value;
            return;
        }
        if (next == null)
            next = new HashMap<>();
        
        if (!next.containsKey(args[index]))
            next.put(args[index], new FunctionCache());
        next.get(args[index]).add(args, index + 1, value);
    }

    private Apcomplex get(Apcomplex[] args, int index) {
        if (args == null || index >= args.length)
            return value;
        
        Apcomplex n = args[index];
        if (next != null && next.containsKey(n))
            return next.get(n).get(args, index + 1);

        return null;
    }

    Apcomplex get(Apcomplex[] args) {
        return get(args, 0);
    }

    @Override
    public String toString() {
        if (next == null && value == null)
            return "";

        Stack<CaseDfs> stack = new Stack<>();
        List<String> result = new ArrayList<>();
        
        stack.push(new CaseDfs(this, "["));
        while (!stack.isEmpty()) {
            CaseDfs a = stack.pop();
            if (a.node.next == null) {
                result.add(a.args + "] = " + a.node.value);
                continue;
            }
            if (a.node.next != null) {
                for (Apcomplex next : a.node.next.keySet()) {
                    String newArgs;
                    if (a.args.equals("["))
                        newArgs = a.args + next;
                    else
                        newArgs = a.args + "," + next;
                    stack.push(new CaseDfs(a.node.next.get(next), newArgs));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String s : result) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    private record CaseDfs(FunctionCache node, String args) {
    }
}