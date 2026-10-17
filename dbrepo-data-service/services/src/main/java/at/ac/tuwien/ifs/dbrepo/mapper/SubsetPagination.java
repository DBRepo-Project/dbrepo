package at.ac.tuwien.ifs.dbrepo.mapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Adjust only the outer bounds; never rewrite temporal table references or ORDER BY. */
final class SubsetPagination {
    // jOOQ's community parser cannot parse the legacy MariaDB FOR SYSTEM_TIME clauses.
    private static final Pattern TOKEN = Pattern.compile(
            "'(?:''|\\\\.|[^'\\\\])*'|\"(?:\"\"|\\\\.|[^\"\\\\])*\"|`(?:``|\\\\.|[^`\\\\])*`"
                    + "|/\\*.*?\\*/|--(?=\\s|$)[^\\r\\n]*|#[^\\r\\n]*"
                    + "|[\\p{L}_][\\p{L}\\p{N}_$]*|[0-9]+|[^\\s]", Pattern.DOTALL);
    private static final BigInteger MAX_OFFSET = new BigInteger("18446744073709551615");

    private SubsetPagination() { }

    static String paginate(String query, Long page, Long size) {
        if (page == null || size == null) return query;
        final List<Token> tokens = outerTokens(query);
        BigInteger offset = BigInteger.valueOf(page).multiply(BigInteger.valueOf(size));
        BigInteger limit = BigInteger.valueOf(size);
        int start = query.length(), end = start;
        for (int i = 0; i < tokens.size(); i++) {
            final Token token = tokens.get(i);
            if (token.text().equals(";")) {
                start = end = token.start();
                break;
            }
            if (!token.text().equalsIgnoreCase("LIMIT") || i + 1 == tokens.size()
                    || !tokens.get(i + 1).text().chars().allMatch(Character::isDigit)) continue;
            start = token.start();
            Token bound = tokens.get(++i);
            BigInteger storedLimit = new BigInteger(bound.text());
            BigInteger storedOffset = BigInteger.ZERO;
            if (i + 1 < tokens.size() && tokens.get(i + 1).text().equals(",")) {
                storedOffset = storedLimit;
                bound = tokens.get(i + 2);
                storedLimit = new BigInteger(bound.text());
            } else if (i + 1 < tokens.size() && tokens.get(i + 1).text().equalsIgnoreCase("OFFSET")) {
                bound = tokens.get(i + 2);
                storedOffset = new BigInteger(bound.text());
            }
            limit = limit.min(storedLimit.subtract(offset).max(BigInteger.ZERO));
            offset = storedOffset.add(offset.min(storedLimit));
            end = bound.end();
            break;
        }
        if (offset.compareTo(MAX_OFFSET) > 0) {
            limit = offset = BigInteger.ZERO;
        }
        final String clause = "LIMIT " + limit + " OFFSET " + offset;
        return query.substring(0, start) + (start == end ? "\n" : "") + clause + query.substring(end);
    }

    private static List<Token> outerTokens(String query) {
        final List<Token> tokens = new ArrayList<>();
        final var matcher = TOKEN.matcher(query);
        int depth = 0;
        while (matcher.find()) {
            final String token = matcher.group();
            if (token.equals("(")) depth++;
            else if (token.equals(")")) depth--;
            else if (depth == 0 && !token.startsWith("'") && !token.startsWith("\"") && !token.startsWith("`")
                    && !token.startsWith("/*") && !token.startsWith("--") && !token.startsWith("#")) {
                tokens.add(new Token(token, matcher.start(), matcher.end()));
            }
        }
        return tokens;
    }

    private record Token(String text, int start, int end) { }
}
