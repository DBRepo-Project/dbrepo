package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.SubsetHistoryIncompleteException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultVisitListener;
import org.jooq.impl.QOM;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** Reproduction metadata contains table-version proofs, never query result rows. */
public final class SubsetHistory {
    private static final ObjectMapper JSON = new ObjectMapper();
    private SubsetHistory() { }

    public record Proof(long count, String hash) { }
    public record Binding(UUID tableId, boolean nativeOnly, Proof visible) { }
    public record Execution(int format, String databaseName, UUID databaseId, long cut,
                            Map<String, Binding> tables, Map<String, String> views) { }
    public record Prepared(String sql, String context) { }

    public static Prepared capture(Connection c, Database db, String site, String query, Instant selected) throws SQLException {
        final Map<String, Table> referenced = new LinkedHashMap<>();
        final Map<String, String> views = new LinkedHashMap<>();
        collect(db, query, views, referenced, new HashSet<>());
        TupleVersionHistory.prepare(c, null);
        for (Table table : referenced.values()) {
            if (hasKey(c, table)) TupleVersionHistory.prepareImported(c, table);
        }
        c.setAutoCommit(false);
        try {
            final Map<String, Binding> bindings = new LinkedHashMap<>();
            // The counter and all table proofs are read from the same InnoDB read view.
            final long cut = TupleVersionHistory.cut(c);
            for (Table table : referenced.values()) {
                final boolean nativeOnly = !hasKey(c, table);
                if (!nativeOnly && (table.getCreationLocation() == null || table.getCreationLocation().equals(site))) {
                    TupleVersionHistory.backfillSource(c, site, db.getId(), table);
                }
                final Proof proof = nativeOnly ? null : proof(c, visible(site, db.getId(), table.getId(), selected, cut));
                bindings.put(table.getInternalName(), new Binding(table.getId(), nativeOnly, proof));
            }
            final Execution execution = new Execution(1, db.getInternalName(), db.getId(), cut, bindings, views);
            final String sql = execute(c, db, site, selected, execution, query, site);
            final String encoded = JSON.writeValueAsString(execution);
            c.commit();
            return new Prepared(sql, encoded);
        } catch (SQLException | RuntimeException e) {
            c.rollback(); throw e;
        } catch (java.io.IOException e) {
            c.rollback(); throw new SQLException("Cannot encode subset execution metadata", e);
        } finally { c.setAutoCommit(true); }
    }

    public static String replay(Connection c, Database db, String origin, Instant selected,
                                String context, String query, String localSite) throws SQLException {
        if (context == null) throw incomplete("This subset has no proven historical version mapping");
        final Execution execution;
        try { execution = JSON.readValue(context, Execution.class); }
        catch (java.io.IOException e) { throw incomplete("Invalid subset execution metadata"); }
        validate(execution);
        return execute(c, db, origin, selected, execution, query, localSite);
    }

    public static void validate(Execution e) {
        if (e == null || e.format() != 1 || e.databaseId() == null || e.databaseName() == null
                || e.cut() < 0 || e.tables() == null || e.views() == null || e.tables().size() + e.views().size() > 256) {
            throw incomplete("Unsupported subset execution metadata");
        }
        for (Binding b : e.tables().values()) {
            if (b == null || b.tableId() == null || (!b.nativeOnly() && (b.visible() == null
                    || b.visible().count() < 0 || b.visible().hash() == null || !b.visible().hash().matches("[0-9a-f]{64}")))) {
                throw incomplete("Invalid table-version proof");
            }
        }
    }

    private static String execute(Connection c, Database db, String site, Instant selected, Execution execution,
                                  String query, String localSite) throws SQLException {
        final Map<String, String> relations = new HashMap<>();
        for (var entry : execution.tables().entrySet()) {
            final String name = entry.getKey();
            final Table table = db.getTables().stream().filter(t -> name.equals(t.getInternalName())).findFirst()
                    .orElseThrow(() -> incomplete("Historical table mapping is missing: " + name));
            final Binding binding = entry.getValue();
            if (binding.nativeOnly()) {
                if (!Objects.equals(site, localSite) || !execution.databaseId().equals(db.getId())) {
                    throw incomplete("An unreplicated native version has no cross-site identity");
                }
                relations.put(name, "SELECT " + columns(c, table, "h", false) + " FROM " + quote(name)
                        + " FOR SYSTEM_TIME AS OF TIMESTAMP " + literal(selected) + " h");
                continue;
            }
            final String visible = visible(site, execution.databaseId(), binding.tableId(), selected, execution.cut());
            if (!binding.visible().equals(proof(c, visible))) throw incomplete("Site visibility evidence is incomplete for " + name);
            final String versions = versions(c, table);
            final String available = "SELECT DISTINCT h.replication_key AS replication_id,h._version_id AS version_id FROM ("
                    + versions + ") h JOIN (" + visible + ") t ON h.replication_key=t.replication_id AND h._version_id=t.version_id";
            if (!binding.visible().equals(proof(c, available))) throw incomplete("Historical tuple values are incomplete for " + name);
            relations.put(name, "SELECT " + columns(c, table, "h", false) + " FROM (" + versions + ") h JOIN ("
                    + visible + ") t ON h.replication_key=t.replication_id AND h._version_id=t.version_id");
        }
        return expand(query, execution.databaseName(), relations, execution.views(), new HashSet<>());
    }

    private static void collect(Database db, String query, Map<String, String> views,
                                Map<String, Table> tables, Set<String> visiting) {
        rewrite(query, db.getInternalName(), name -> {
            final var table = db.getTables().stream().filter(t -> name.equals(t.getInternalName())).findFirst();
            if (table.isPresent()) tables.put(name, table.get());
            else {
                final var view = db.getViews() == null ? Optional.<at.ac.tuwien.ifs.dbrepo.core.entity.cache.View>empty()
                        : db.getViews().stream().filter(v -> name.equals(v.getInternalName())).findFirst();
                if (view.isEmpty() || !visiting.add(name)) throw incomplete("Unknown or cyclic historical relation: " + name);
                views.put(name, view.get().getQuery());
                collect(db, view.get().getQuery(), views, tables, visiting);
                visiting.remove(name);
            }
            return "SELECT 1";
        });
    }

    private static String expand(String query, String database, Map<String, String> tables,
                                  Map<String, String> views, Set<String> visiting) {
        return rewrite(query, database, name -> {
            if (tables.containsKey(name)) return tables.get(name);
            if (!views.containsKey(name) || !visiting.add(name)) throw incomplete("Historical relation is not bound: " + name);
            final String result = expand(views.get(name), database, tables, views, visiting);
            visiting.remove(name);
            return result;
        });
    }

    /** Visit named table declarations, including those inside joins and nested selects. */
    static String rewrite(String query, String database, Function<String, String> relation) {
        final DSLContext parser = DSL.using(SQLDialect.MARIADB);
        final var select = parser.parser().parseSelect(query);
        final var config = parser.configuration().derive(new DefaultVisitListener() {
            @Override public void visitStart(VisitContext ctx) {
                if (ctx.queryPart() instanceof org.jooq.Table<?> table && ctx.renderContext().declareTables()
                        && !(table instanceof QOM.TableAlias<?>) && table.getQualifiedName().last() != null
                        && table.getClass().getSimpleName().equals("TableImpl")) {
                    final String[] parts = table.getQualifiedName().getName();
                    if (parts.length > 2 || (parts.length == 2 && !parts[0].equals(database))) {
                        throw incomplete("Cross-database references are not supported for historical subsets");
                    }
                    final String sql = relation.apply(table.getName());
                    final var replacement = DSL.table("({0})", DSL.sql(sql));
                    final boolean aliased = Arrays.stream(ctx.queryParts()).anyMatch(p -> p instanceof QOM.TableAlias<?> a
                            && a.$table() == table);
                    ctx.queryPart(aliased ? replacement : replacement.as(table.getName()));
                } else if (ctx.queryPart() instanceof Field<?> field && field.getQualifiedName().getName().length == 3
                        && field.getQualifiedName().first().equals(database)) {
                    final String[] parts = field.getQualifiedName().getName();
                    ctx.queryPart(DSL.field(DSL.name(parts[1], parts[2]), field.getDataType()));
                }
            }
        });
        return DSL.using(config).renderInlined(select);
    }

    private static String visible(String site, UUID database, UUID table, Instant selected, long cut) {
        return "SELECT DISTINCT replication_id,version_id FROM tuple_replication_timestamps WHERE site_url=" + literal(site)
                + " AND database_id=" + literal(database.toString()) + " AND table_id=" + literal(table.toString())
                + " AND row_start <= " + literal(selected) + " AND (visibility_start IS NULL OR visibility_start <= " + cut + ")"
                + " AND (row_end IS NULL OR row_end > " + literal(selected) + " OR visibility_end > " + cut + ")";
    }

    private static Proof proof(Connection c, String sql) throws SQLException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long count = 0;
            try (var s = c.createStatement(); var rows = s.executeQuery("SELECT replication_id,version_id FROM (" + sql
                    + ") p ORDER BY BINARY replication_id,BINARY version_id")) {
                while (rows.next()) {
                    if (rows.getString(2) == null) throw incomplete("A visible tuple has no proven version identity");
                    for (int i = 1; i <= 2; i++) {
                        final byte[] value = rows.getString(i).getBytes(StandardCharsets.UTF_8);
                        digest.update(ByteBuffer.allocate(4).putInt(value.length).array()); digest.update(value);
                    }
                    count++;
                }
            }
            return new Proof(count, HexFormat.of().formatHex(digest.digest()));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String versions(Connection c, Table table) throws SQLException {
        final String nativeColumns = columns(c, table, "h", true);
        final String importedColumns = columns(c, table, "i", true);
        return "SELECT " + nativeColumns + ",m.version_id AS _version_id FROM " + quote(table.getInternalName())
                + " FOR SYSTEM_TIME ALL h JOIN tuple_replication_versions m ON m.table_id=" + literal(table.getId().toString())
                + " AND m.replication_id=h.replication_key AND m.native_start=h.ROW_START"
                + " UNION ALL SELECT " + importedColumns + ",i._version_id FROM " + quote(TupleVersionHistory.importedName(table))
                + " i WHERE NOT EXISTS (SELECT 1 FROM tuple_replication_versions m WHERE m.table_id="
                + literal(table.getId().toString()) + " AND m.version_id=i._version_id)";
    }

    private static String columns(Connection c, Table table, String alias, boolean internal) throws SQLException {
        final List<String> columns = new ArrayList<>();
        try (var s = c.prepareStatement("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"
                + " AND TABLE_NAME=? ORDER BY ORDINAL_POSITION")) {
            s.setString(1, table.getInternalName());
            try (var rows = s.executeQuery()) {
                while (rows.next()) {
                    final String name = rows.getString(1);
                    if (!name.equalsIgnoreCase("ROW_START") && !name.equalsIgnoreCase("ROW_END")
                            && (internal || !name.equals("replication_key"))) columns.add(alias + "." + quote(name));
                }
            }
        }
        if (columns.isEmpty()) throw incomplete("Historical table schema is unavailable");
        return String.join(",", columns);
    }

    private static boolean hasKey(Connection c, Table table) throws SQLException {
        try (var s = c.prepareStatement("SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"
                + " AND TABLE_NAME=? AND COLUMN_NAME='replication_key'")) {
            s.setString(1, table.getInternalName()); try (var r = s.executeQuery()) { return r.next(); }
        }
    }

    private static String quote(String name) { return TupleVersionHistory.quote(name); }
    private static String literal(String value) { return DSL.using(SQLDialect.MARIADB).renderInlined(DSL.inline(value)); }
    private static String literal(Instant value) {
        return literal(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSS")
                .withZone(java.time.ZoneOffset.UTC).format(value));
    }
    private static SubsetHistoryIncompleteException incomplete(String message) { return new SubsetHistoryIncompleteException(message); }
}
