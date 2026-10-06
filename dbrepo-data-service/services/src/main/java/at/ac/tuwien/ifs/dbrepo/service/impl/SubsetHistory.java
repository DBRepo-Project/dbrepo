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
            if (hasKey(c, table)) {
                TupleVersionHistory.prepareImported(c, table);
                prepareDomains(c, table);
            }
        }
        final int isolation = c.getTransactionIsolation();
        c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        c.setAutoCommit(false);
        try {
            final Map<String, Binding> bindings = new LinkedHashMap<>();
            // The counter and all table proofs are read from the same InnoDB read view.
            final long cut = TupleVersionHistory.cut(c);
            for (Table table : referenced.values()) {
                final boolean nativeOnly = !hasKey(c, table);
                if (!nativeOnly) {
                    TupleVersionHistory.backfillLocal(c, site, db.getId(), table,
                            table.getCreationLocation() == null || table.getCreationLocation().equals(site));
                    requireNativeCoverage(c, table, selected);
                }
                final Proof proof = nativeOnly ? null : proof(c, visible(site, db.getId(), table.getId(), selected, cut));
                bindings.put(table.getInternalName(), new Binding(table.getId(), nativeOnly, proof));
            }
            final Execution execution = new Execution(2, db.getInternalName(), db.getId(), cut, bindings, views);
            final String sql = execute(c, db, site, selected, execution, query, site);
            final String encoded = JSON.writeValueAsString(execution);
            c.commit();
            return new Prepared(sql, encoded);
        } catch (SQLException | RuntimeException e) {
            c.rollback(); throw e;
        } catch (java.io.IOException e) {
            c.rollback(); throw new SQLException("Cannot encode subset execution metadata", e);
        } finally { c.setAutoCommit(true); c.setTransactionIsolation(isolation); }
    }

    public static String replay(Connection c, Database db, String origin, Instant selected,
                                String context, String query, String localSite) throws SQLException {
        if (context == null) throw incomplete("This subset has no proven historical version mapping");
        final Execution execution = decode(context);
        try (var s = c.createStatement()) { s.execute("SET time_zone='+00:00'"); }
        for (Binding binding : execution.tables().values()) {
            if (!binding.nativeOnly()) prepareDomains(c, table(db, origin, execution, binding));
        }
        return execute(c, db, origin, selected, execution, query, localSite);
    }

    public static Execution decode(String context) {
        final Execution execution;
        try { execution = JSON.readValue(context, Execution.class); }
        catch (java.io.IOException e) { throw incomplete("Invalid subset execution metadata"); }
        validate(execution);
        return execution;
    }

    public static void validate(Execution e) {
        if (e == null || e.format() != 2 || e.databaseId() == null || e.databaseName() == null
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
            final Binding binding = entry.getValue();
            final Table table = table(db, site, execution, binding);
            if (binding.nativeOnly()) {
                if (!Objects.equals(site, localSite) || !execution.databaseId().equals(db.getId())) {
                    throw incomplete("An unreplicated native version has no cross-site identity");
                }
                relations.put(name, "SELECT " + columns(c, table, "h", false) + " FROM " + quote(table.getInternalName())
                        + " FOR SYSTEM_TIME AS OF TIMESTAMP " + literal(selected) + " h");
                continue;
            }
            final String visible = visible(site, execution.databaseId(), binding.tableId(), selected, execution.cut());
            if (!binding.visible().equals(proof(c, visible))) throw incomplete("Site visibility evidence is incomplete for " + name);
            final String versions = versions(c, table);
            final String available = "SELECT DISTINCT h.replication_key AS replication_id,h._master_site_ts AS master_site_ts FROM ("
                    + versions + ") h JOIN (" + visible + ") t ON h.replication_key=t.replication_id AND h._master_site_ts=t.master_site_ts";
            if (!binding.visible().equals(proof(c, available))) throw incomplete("Historical tuple values are incomplete for " + name);
            final Map<String,String> projection = new HashMap<>();
            final StringBuilder joins = new StringBuilder();
            int index = 0;
            for (var domain : domains(c, table).entrySet()) {
                try (var s = c.createStatement()) {
                    s.executeUpdate("INSERT IGNORE INTO " + quote(domain.getValue()) + " (value) SELECT DISTINCT h."
                            + quote(domain.getKey()) + " FROM (" + versions + ") h WHERE h." + quote(domain.getKey()) + " IS NOT NULL");
                }
                final String alias = "d" + index++;
                projection.put(domain.getKey(), alias + ".value AS " + quote(domain.getKey()));
                joins.append(" LEFT JOIN ").append(quote(domain.getValue())).append(" ").append(alias)
                        .append(" ON BINARY ").append(alias).append(".value=BINARY h.").append(quote(domain.getKey()));
            }
            relations.put(name, "SELECT " + columns(c, table, "h", false, projection) + " FROM (" + versions + ") h JOIN ("
                    + visible + ") t ON h.replication_key=t.replication_id AND h._master_site_ts=t.master_site_ts" + joins);
        }
        return expand(query, execution.databaseName(), relations, execution.views(), new HashSet<>());
    }

    public static Table table(Database db, String origin, Execution execution, Binding binding) {
        return db.getTables().stream().filter(t -> execution.databaseId().equals(db.getId())
                ? binding.tableId().equals(t.getId())
                : t.getReplicaUrls() != null && binding.tableId().equals(t.getReplicaUrls().get(origin))).findFirst()
                .orElseThrow(() -> incomplete("Historical table identity mapping is missing"));
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
                } else if (ctx.queryPart() instanceof QualifiedAsterisk star
                        && star.qualifier().getQualifiedName().getName().length == 2
                        && star.qualifier().getQualifiedName().first().equals(database)) {
                    ctx.queryPart(DSL.table(DSL.name(star.qualifier().getName())).asterisk().except(star.$except()));
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
        return "SELECT DISTINCT replication_id,master_site_ts FROM tuple_replication_timestamps WHERE site_url=" + literal(site)
                + " AND database_id=" + literal(database.toString()) + " AND table_id=" + literal(table.toString())
                + " AND row_start <= " + literal(selected) + " AND (visibility_start IS NULL OR visibility_start <= " + cut + ")"
                + " AND (row_end IS NULL OR row_end > " + literal(selected) + " OR visibility_end > " + cut + ")";
    }

    private static Proof proof(Connection c, String sql) throws SQLException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long count = 0;
            try (var s = c.createStatement(); var rows = s.executeQuery("SELECT replication_id,master_site_ts FROM (" + sql
                    + ") p ORDER BY BINARY replication_id,BINARY master_site_ts")) {
                while (rows.next()) {
                    if (rows.getString(2) == null) throw incomplete("A visible tuple has no proven version identity");
                    for (int i = 1; i <= 2; i++) {
                        final byte[] value = (i == 1 ? rows.getString(i)
                                : rows.getTimestamp(i, TupleVersionHistory.utc()).toInstant().toString()).getBytes(StandardCharsets.UTF_8);
                        digest.update(ByteBuffer.allocate(4).putInt(value.length).array()); digest.update(value);
                    }
                    count++;
                }
            }
            return new Proof(count, HexFormat.of().formatHex(digest.digest()));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static void requireNativeCoverage(Connection c, Table table, Instant selected) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT 1 FROM " + quote(table.getInternalName())
                + " FOR SYSTEM_TIME AS OF TIMESTAMP " + literal(selected) + " h LEFT JOIN tuple_replication_versions m"
                + " ON m.table_id=" + literal(table.getId().toString())
                + " AND m.replication_id=h.replication_key AND m.native_start=h.ROW_START WHERE m.master_site_ts IS NULL LIMIT 1")) {
            if (r.next()) throw incomplete("Visible native history has no proven version identity: " + table.getInternalName());
        }
    }

    private static String versions(Connection c, Table table) throws SQLException {
        final String nativeColumns = columns(c, table, "h", true);
        final String importedColumns = columns(c, table, "i", true);
        return "SELECT " + nativeColumns + ",m.master_site_ts AS _master_site_ts FROM " + quote(table.getInternalName())
                + " FOR SYSTEM_TIME ALL h JOIN tuple_replication_versions m ON m.table_id=" + literal(table.getId().toString())
                + " AND m.replication_id=h.replication_key AND m.native_start=h.ROW_START AND m.master_site_ts IS NOT NULL"
                + " UNION ALL SELECT " + importedColumns + ",i._master_site_ts FROM " + quote(TupleVersionHistory.importedName(table))
                + " i WHERE i._master_site_ts IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tuple_replication_versions m WHERE m.table_id="
                + literal(table.getId().toString()) + " AND m.replication_id=i.replication_key AND m.master_site_ts=i._master_site_ts)";
    }

    private static String columns(Connection c, Table table, String alias, boolean internal) throws SQLException {
        return columns(c, table, alias, internal, Map.of());
    }

    private static String columns(Connection c, Table table, String alias, boolean internal,
                                   Map<String,String> projection) throws SQLException {
        final List<String> columns = new ArrayList<>();
        try (var s = c.prepareStatement("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"
                + " AND TABLE_NAME=? ORDER BY ORDINAL_POSITION")) {
            s.setString(1, table.getInternalName());
            try (var rows = s.executeQuery()) {
                while (rows.next()) {
                    final String name = rows.getString(1);
                    if (!name.equalsIgnoreCase("ROW_START") && !name.equalsIgnoreCase("ROW_END")
                            && (internal || !name.equals("replication_key"))) {
                        columns.add(projection.getOrDefault(name, alias + "." + quote(name)));
                    }
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

    /** A typed domain lookup preserves ENUM/SET ordinal semantics, which UNION converts to text. */
    private static void prepareDomains(Connection c, Table table) throws SQLException {
        for (var domain : domains(c, table).entrySet()) {
            try (var s = c.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS " + quote(domain.getValue()) + " AS SELECT "
                        + quote(domain.getKey()) + " AS value FROM " + quote(table.getInternalName()) + " WHERE FALSE");
                s.execute("ALTER TABLE " + quote(domain.getValue()) + " ADD UNIQUE INDEX IF NOT EXISTS domain_value(value)");
            }
        }
    }

    private static Map<String,String> domains(Connection c, Table table) throws SQLException {
        final Map<String,String> domains = new LinkedHashMap<>();
        try (var s = c.prepareStatement("SELECT COLUMN_NAME,ORDINAL_POSITION FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND DATA_TYPE IN ('enum','set') ORDER BY ORDINAL_POSITION")) {
            s.setString(1, table.getInternalName());
            try (var rows = s.executeQuery()) {
                while (rows.next()) domains.put(rows.getString(1), "_dbrepo_domain_"
                        + table.getId().toString().replace("-", "") + "_" + rows.getInt(2));
            }
        }
        return domains;
    }

    private static String quote(String name) { return TupleVersionHistory.quote(name); }
    private static String literal(String value) { return DSL.using(SQLDialect.MARIADB).renderInlined(DSL.inline(value)); }
    private static String literal(Instant value) {
        return literal(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSS")
                .withZone(java.time.ZoneOffset.UTC).format(value));
    }
    private static SubsetHistoryIncompleteException incomplete(String message) { return new SubsetHistoryIncompleteException(message); }
}
