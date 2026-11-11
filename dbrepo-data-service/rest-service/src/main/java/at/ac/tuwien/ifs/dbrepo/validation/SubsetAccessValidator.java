package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetHistory;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import org.jooq.Clause;
import org.jooq.Name;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.Sequence;
import org.jooq.conf.ParseUnsupportedSyntax;
import org.jooq.conf.ParseUnknownFunctions;
import org.jooq.conf.Settings;
import org.jooq.VisitListener;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Checks subset sources against a fresh metadata snapshot before any data is read. */
@Component
public class SubsetAccessValidator {

    private static final Set<String> GROUPS = Set.of("IN", "EXISTS", "AND", "OR", "NOT", "WHERE", "ON", "FROM",
            "JOIN", "SELECT", "DISTINCT", "AS", "UNION", "ALL");

    public void validateStored(Database database, Subset subset, Principal principal) throws NotAllowedException {
        if (AuthUtil.isSystem(principal)) return;
        if (subset.getExecutionContext() == null) {
            validate(database, subset.getQueryNormalized(), principal);
            return;
        }
        final Database sources;
        try {
            final var execution = SubsetHistory.decode(subset.getExecutionContext());
            // Stored SQL uses origin names; permissions always come from the current local catalog.
            final var tables = execution.tables().entrySet().stream().map(entry -> {
                final var local = SubsetHistory.table(database, subset.getCreationLocation(), execution, entry.getValue());
                return at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
                        .id(local.getId()).internalName(entry.getKey()).isPublic(local.getIsPublic()).build();
            }).toList();
            sources = Database.builder().id(database.getId()).internalName(execution.databaseName())
                    .accesses(database.getAccesses()).tables(tables).views(database.getViews()).build();
        } catch (RuntimeException error) {
            throw denied();
        }
        validate(sources, subset.getQueryNormalized(), principal);
    }

    public void validate(Database database, SubsetDto subset, Principal principal) throws NotAllowedException {
        if (AuthUtil.isSystem(principal)) {
            return;
        }
        if (subset.getDatasourceIds() == null || subset.getDatasourceIds().isEmpty()) {
            throw denied();
        }
        final Set<UUID> sources = new HashSet<>(subset.getDatasourceIds());
        if (subset.getJoins() != null) {
            subset.getJoins().forEach(join -> sources.add(join.getDatasourceId()));
        }
        final boolean granted = hasAccess(database, principal);
        for (UUID source : sources) {
            final boolean readableTable = database.getTables().stream().anyMatch(table ->
                    table.getId().equals(source) &&
                            (Boolean.TRUE.equals(table.getIsPublic()) || granted));
            final boolean readableView = database.getViews().stream().anyMatch(view ->
                    view.getId().equals(source) &&
                            (Boolean.TRUE.equals(view.getIsPublic()) || granted));
            if (!readableTable && !readableView) {
                throw denied();
            }
        }
    }

    public void validate(Database database, String normalizedQuery, Principal principal) throws NotAllowedException {
        if (AuthUtil.isSystem(principal)) {
            return;
        }
        final Set<Name> sources = new HashSet<>();
        try {
            final var select = DSL.using(SQLDialect.MARIADB, new Settings()
                    .withParseDialect(SQLDialect.MARIADB)
                    .withParseUnsupportedSyntax(ParseUnsupportedSyntax.FAIL)
                    .withParseUnknownFunctions(ParseUnknownFunctions.FAIL)).parser().parseSelect(analysisQuery(normalizedQuery));
            final DefaultConfiguration configuration = new DefaultConfiguration();
            configuration.set(SQLDialect.MARIADB);
            configuration.set(VisitListener.onVisitStart(context -> {
                if (context.queryPart() instanceof Sequence<?>) {
                    throw new IllegalArgumentException("Unsupported sequence");
                }
                final Clause[] clauses = context.clauses();
                if (context.queryPart() instanceof Table<?> table && clauses.length > 0 &&
                        clauses[clauses.length - 1] == Clause.TABLE_REFERENCE) {
                    sources.add(table.getQualifiedName());
                }
            }));
            DSL.using(configuration).render(select);
        } catch (RuntimeException e) {
            throw denied();
        }
        if (sources.isEmpty()) {
            throw denied();
        }
        final boolean granted = hasAccess(database, principal);
        for (Name source : sources) {
            final String[] parts = source.getName();
            if (parts.length > 2 || (parts.length == 2 && !parts[0].equals(database.getInternalName()))) {
                throw denied();
            }
            final String name = source.last();
            // Preserve case: MariaDB table names can be case-sensitive.
            final boolean readableTable = database.getTables().stream().anyMatch(table ->
                    table.getInternalName().equals(name) &&
                            (Boolean.TRUE.equals(table.getIsPublic()) || granted));
            final boolean readableView = database.getViews().stream().anyMatch(view ->
                    view.getInternalName().equals(name) &&
                            (Boolean.TRUE.equals(view.getIsPublic()) || granted));
            if (!readableTable && !readableView) {
                throw denied();
            }
        }
    }

    private boolean hasAccess(Database database, Principal principal) {
        return principal != null && database.getAccesses().stream()
                .anyMatch(access -> access.getUsername().equals(AuthUtil.getUsername(principal)));
    }

    private NotAllowedException denied() {
        return new NotAllowedException("Subset sources are unavailable or not readable");
    }

    /**
     * The OSS parser cannot read MariaDB's temporal clause. Remove only the generated literal form for analysis;
     * execution always uses the original query. Legacy SQL outside this supported read-only subset fails closed.
     */
    private String analysisQuery(String sql) {
        final List<Token> tokens = tokenize(sql);
        final StringBuilder analysis = new StringBuilder(sql);
        if (!tokens.isEmpty() && tokens.getLast().text().equals(";")) {
            analysis.setCharAt(tokens.getLast().start(), ' ');
        }
        for (int i = 0; i < tokens.size(); i++) {
            final Token token = tokens.get(i);
            final String word = token.text().toUpperCase(Locale.ROOT);
            if (word.equals("FOR")) {
                if (i + 5 >= tokens.size() || !tokens.get(i + 1).text().equalsIgnoreCase("SYSTEM_TIME") ||
                        !tokens.get(i + 2).text().equalsIgnoreCase("AS") ||
                        !tokens.get(i + 3).text().equalsIgnoreCase("OF") ||
                        !tokens.get(i + 4).text().equalsIgnoreCase("TIMESTAMP") ||
                        !tokens.get(i + 5).text().matches("'[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{6})?'")) {
                    throw new IllegalArgumentException("Unsupported temporal or locking clause");
                }
                for (int j = token.start(); j < tokens.get(i + 5).end(); j++) {
                    analysis.setCharAt(j, ' ');
                }
                i += 5;
            } else if (word.equals("INTO") || word.equals("LOCK") || word.equals("@")) {
                throw new IllegalArgumentException("Unsupported query clause");
            } else if (i + 1 < tokens.size() && tokens.get(i + 1).text().equals("(") && token.identifier()) {
                if (!GROUPS.contains(word) ||
                        (i > 0 && tokens.get(i - 1).text().equals("."))) {
                    throw new IllegalArgumentException("Unsupported function");
                }
            }
        }
        return analysis.toString();
    }

    private List<Token> tokenize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Missing query");
        }
        final List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < sql.length();) {
            final char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            final int start = i++;
            final boolean identifier = Character.isLetterOrDigit(c) || c == '_' || c == '`';
            if (c == '\'' || c == '`') {
                boolean closed = false;
                while (i < sql.length()) {
                    final char next = sql.charAt(i++);
                    if (next == '\\') {
                        // NO_BACKSLASH_ESCAPES changes quote boundaries. Do not guess the connection SQL mode.
                        throw new IllegalArgumentException("Unsupported backslash escape");
                    } else if (next == c) {
                        if (i < sql.length() && sql.charAt(i) == c) {
                            i++;
                        } else {
                            closed = true;
                            break;
                        }
                    }
                }
                if (!closed) {
                    throw new IllegalArgumentException("Unterminated quoted token");
                }
            } else if (c == '"' || c == '#' ||
                    (i < sql.length() && ((c == '/' && sql.charAt(i) == '*') || (c == '-' && sql.charAt(i) == '-')))) {
                // Generated SQL has no comments. Reject all forms, including MariaDB executable comments.
                throw new IllegalArgumentException("Unsupported comment or quoting");
            } else if (identifier) {
                while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_' || sql.charAt(i) == '$')) {
                    i++;
                }
            }
            tokens.add(new Token(sql.substring(start, i), start, i, identifier));
        }
        return tokens;
    }

    private record Token(String text, int start, int end, boolean identifier) {
    }
}
