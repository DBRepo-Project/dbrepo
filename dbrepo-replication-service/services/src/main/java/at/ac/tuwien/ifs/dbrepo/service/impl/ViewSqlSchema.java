package at.ac.tuwien.ifs.dbrepo.service.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Localizes identifiers in MariaDB's stored view SQL without editing literals or comments. */
final class ViewSqlSchema {
    private static final Pattern TOKENS = Pattern.compile(
            "'(?:\\\\.|''|[^'\\\\])*'|\"(?:\\\\.|\"\"|[^\"\\\\])*\"|`(?:``|[^`])*`"
                    + "|/\\*.*?\\*/|--(?=[\\s\\p{Cntrl}])[^\\r\\n]*|#[^\\r\\n]*"
                    + "|[\\p{L}\\p{N}_$]+|\\s+|.", Pattern.DOTALL);
    private static final Set<String> END_FROM = Set.of("where", "group", "having", "order", "limit", "union", "window", "select");
    private static final Set<String> RELATION_START = Set.of("from", "join", "straight_join", ",", "(", "lateral");

    private ViewSqlSchema() {
    }

    static String localize(String query, String sourceSchema) {
        if (query == null || sourceSchema == null || sourceSchema.isBlank()) {
            throw new IllegalArgumentException("View replication requires SQL and the source database name");
        }
        final List<String> tokens = new ArrayList<>();
        TOKENS.matcher(query).results().forEach(match -> tokens.add(match.group()));
        final boolean[] removed = new boolean[tokens.size()];
        final ArrayDeque<Boolean> scopes = new ArrayDeque<>();
        boolean from = false;
        String previous = "";
        for (int i = 0; i < tokens.size(); i++) {
            final String token = tokens.get(i);
            if (trivia(token)) {
                continue;
            }
            final String lower = token.toLowerCase(Locale.ROOT);
            if (token.equals("(")) {
                scopes.push(from);
                from = from && RELATION_START.contains(previous);
            } else if (token.equals(")")) {
                from = !scopes.isEmpty() && scopes.pop();
            } else if (lower.equals("from") || lower.equals("join") || lower.equals("straight_join")) {
                from = true;
            } else if (END_FROM.contains(lower)) {
                from = false;
            }
            if (identifier(token) && unquote(token).equals(sourceSchema) && !previous.equals(".")) {
                final int dot = next(tokens, i);
                final int relation = next(tokens, dot);
                final int suffix = next(tokens, relation);
                // Three-part columns have a schema; two-part alias.column references do not.
                if (dot < tokens.size() && tokens.get(dot).equals(".")
                        && relation < tokens.size() && identifier(tokens.get(relation))
                        && (suffix < tokens.size() && tokens.get(suffix).equals(".")
                        || from && RELATION_START.contains(previous))) {
                    removed[i] = true;
                    removed[dot] = true;
                }
            }
            previous = lower;
        }
        final StringBuilder localized = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (!removed[i]) {
                localized.append(tokens.get(i));
            }
        }
        return localized.toString();
    }

    private static int next(List<String> tokens, int index) {
        do {
            index++;
        } while (index < tokens.size() && trivia(tokens.get(index)));
        return index;
    }

    private static boolean trivia(String token) {
        return token.isBlank() || token.startsWith("/*") || token.startsWith("--") || token.startsWith("#");
    }

    private static boolean identifier(String token) {
        return token.startsWith("`") || Character.isLetterOrDigit(token.charAt(0)) || "_$".indexOf(token.charAt(0)) >= 0;
    }

    private static String unquote(String token) {
        return token.startsWith("`") ? token.substring(1, token.length() - 1).replace("``", "`") : token;
    }
}
