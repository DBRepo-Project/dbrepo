package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubsetAccessValidatorUnitTest extends BaseTest {
    private final SubsetAccessValidator validator = new SubsetAccessValidator();

    @Test
    void storedReplicaQueryUsesCurrentLocalPermissionsAndOriginIdentity() throws Exception {
        final UUID originDatabase = UUID.randomUUID(), originTable = UUID.randomUUID();
        final String origin = "https://origin.example";
        final Table local = table("local_rows", true);
        local.setReplicaUrls(java.util.Map.of(origin, originTable));
        database.setTables(List.of(local));
        final var execution = new at.ac.tuwien.ifs.dbrepo.service.impl.SubsetHistory.Execution(
                2, "origin_database", originDatabase, 0,
                java.util.Map.of("origin_rows", new at.ac.tuwien.ifs.dbrepo.service.impl.SubsetHistory.Binding(
                        originTable, true, null)), java.util.Map.of());
        final var subset = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset.builder()
                .creationLocation(origin).queryNormalized("SELECT * FROM origin_database.origin_rows")
                .executionContext(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(execution)).build();
        assertDoesNotThrow(() -> validator.validateStored(database, subset, null));
        local.setIsPublic(false);
        assertThrows(NotAllowedException.class, () -> validator.validateStored(database, subset, null));
        local.setIsPublic(true);
        local.setReplicaUrls(java.util.Map.of(origin, UUID.randomUUID()));
        assertThrows(NotAllowedException.class, () -> validator.validateStored(database, subset, null));
        local.setReplicaUrls(java.util.Map.of(origin, originTable));
        subset.setQueryNormalized("SELECT * FROM another_database.origin_rows");
        assertThrows(NotAllowedException.class, () -> validator.validateStored(database, subset, null));
    }
    private final Database database = Database.builder().id(UUID.randomUUID()).internalName("testdb").accesses(List.of())
            .tables(List.of(table("public_rows", true), table("secret", false), table("Secret", true),
                    table("we`ird", true)))
            .views(List.of(View.builder().id(UUID.randomUUID()).internalName("public_view")
                    .isPublic(true).query("SELECT * FROM secret").build(),
                    View.builder().id(UUID.randomUUID()).internalName("private_view")
                            .isPublic(false).query("SELECT * FROM public_rows").build())).build();

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT id FROM public_rows",
            "SELECT id FROM `testdb`.`public_rows`",
            "SELECT a.id FROM public_rows a JOIN Secret b ON a.id = b.id",
            "SELECT * FROM (SELECT * FROM public_rows) a",
            "SELECT * FROM public_rows WHERE EXISTS (SELECT 1 FROM Secret)",
            "SELECT * FROM public_rows UNION SELECT * FROM Secret",
            "SELECT * FROM public_rows WHERE id IN (1, 2) AND (id = 1 OR id = 2)",
            "SELECT * FROM public_rows FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000'",
            "SELECT * FROM public_rows FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47' JOIN Secret ON public_rows.id = Secret.id",
            "SELECT 'FOR SYSTEM_TIME AS OF TIMESTAMP ''2025-07-01 09:44:47''', '/*!', '/*M!', '--' FROM public_rows",
            "SELECT * FROM `we``ird`",
            "SELECT * FROM public_view",
            "SELECT * FROM Secret;"
    })
    void readableSources_succeed(String query) {
        assertDoesNotThrow(() -> validator.validate(database, query, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM secret",
            "SELECT * FROM private_view",
            "SELECT * FROM public_rows JOIN secret ON public_rows.id = secret.id",
            "SELECT * FROM (SELECT * FROM secret) a",
            "SELECT (SELECT id FROM secret) FROM public_rows",
            "SELECT * FROM public_rows WHERE EXISTS (SELECT 1 FROM secret)",
            "SELECT * FROM public_rows UNION SELECT * FROM secret",
            "SELECT * FROM other.public_rows",
            "SELECT * FROM `other`.`public_rows`",
            "SELECT * FROM `Testdb`.`public_rows`",
            "SELECT * FROM `testdb`.`secret`",
            "SELECT * FROM PUBLIC_ROWS",
            "SELECT * FROM missing",
            "SELECT 1",
            "SELECT * FROM public_rows; SELECT * FROM secret",
            "SELECT * FROM public_rows garbage garbage",
            "SELECT * INTO OUTFILE '/tmp/out' FROM public_rows",
            "SELECT * FROM public_rows INTO DUMPFILE '/tmp/out'",
            "SELECT LOAD_FILE('/etc/passwd') FROM public_rows",
            "SELECT testdb.custom(id) FROM public_rows",
            "SELECT custom(id) FROM public_rows",
            "SELECT SLEEP(1) FROM public_rows",
            "SELECT COUNT(id) FROM public_rows",
            "SELECT COUNT (id) FROM public_rows",
            "SELECT SUM (id) FROM public_rows",
            "SELECT MIN (id) FROM public_rows",
            "SELECT MAX (id) FROM public_rows",
            "SELECT UPPER(name) FROM public_rows",
            "SELECT NEXT VALUE FOR other.seq FROM public_rows",
            "SELECT NEXTVAL(other.seq) FROM public_rows",
            "SELECT * FROM public_rows FOR UPDATE",
            "SELECT * FROM public_rows LOCK IN SHARE MODE",
            "SELECT * FROM public_rows /*!50000 JOIN secret ON 1=1 */",
            "SELECT * FROM public_rows /*M! JOIN secret ON 1=1 */",
            "SELECT * FROM public_rows /* FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47' */",
            "SELECT SLEEP/**/(1) FROM public_rows",
            "SELECT * FROM public_rows -- hidden\n JOIN secret ON 1=1",
            "SELECT * FROM public_rows # hidden\n JOIN secret ON 1=1",
            "SELECT * FROM public_rows FOR SYSTEM_TIME ALL",
            "SELECT * FROM public_rows FOR SYSTEM_TIME AS OF TIMESTAMP custom()",
            "SELECT * FROM public_rows FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47' JOIN secret ON 1=1",
            "SELECT 'a\\' FROM public_rows",
            "SELECT 'a\\\\' FROM public_rows",
            "SELECT 'unterminated FROM public_rows",
            "SELECT * FROM \"public_rows\"",
            "SELECT @variable FROM public_rows"
    })
    void unreadableOrUnsupportedSources_fail(String query) {
        assertThrows(NotAllowedException.class, () -> validator.validate(database, query, null));
    }

    @Test
    void databaseGrantDoesNotAllowForeignOrUnknownSources() {
        database.setAccesses(DATABASE_1_CACHE.getAccesses());
        assertDoesNotThrow(() -> validator.validate(database, "SELECT * FROM secret", USER_1_PRINCIPAL));
        assertThrows(NotAllowedException.class, () -> validator.validate(database, "SELECT * FROM other.public_rows", USER_1_PRINCIPAL));
        assertThrows(NotAllowedException.class, () -> validator.validate(database, "SELECT * FROM missing", USER_1_PRINCIPAL));
    }

    private Table table(String name, boolean isPublic) {
        return Table.builder().id(UUID.randomUUID()).internalName(name).isPublic(isPublic).build();
    }
}
