package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DataMapperConstraintIntegrationTest {

    private final DataMapper mapper = new DataMapperImpl();

    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"DESCRIBE sample", "DESCRIBE (SELECT * FROM sample)"})
    void mapsNativeNullabilityAndKeys(String query) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:duckdb:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (regular INTEGER, required VARCHAR NOT NULL, " +
                    "\"YES\" INTEGER NOT NULL, primary_id BIGINT PRIMARY KEY, unique_value INTEGER UNIQUE)");

            final Map<String, ColumnAnalysisResultDto> columns;
            try (var rows = statement.executeQuery(query)) {
                columns = mapper.resultSetToConstraintResult(rows);
            }

            assertEquals(Set.of("regular", "required", "YES", "primary_id", "unique_value"), columns.keySet());
            assertAll(
                    () -> assertColumn(columns.get("regular"), "regular", ColumnTypeDto.INT, true, false),
                    () -> assertColumn(columns.get("required"), "required", ColumnTypeDto.VARCHAR, false, false),
                    () -> assertColumn(columns.get("YES"), "YES", ColumnTypeDto.INT, false, false),
                    () -> assertColumn(columns.get("primary_id"), "primary_id", ColumnTypeDto.BIGINT, false, true),
                    () -> assertColumn(columns.get("unique_value"), "unique_value", ColumnTypeDto.INT, true, false));
        }
    }

    @Test
    void mapsBoundCsvDescribeAsNullableWithoutPrimaryKeys() throws Exception {
        final Path csv = directory.resolve("sample.csv");
        Files.writeString(csv, "id,YES\n1,10\n2,\n");
        try (var connection = DriverManager.getConnection("jdbc:duckdb:");
             var statement = connection.prepareStatement(new DuckDbMapperImpl().queryToRawDescribeS3TableQuery())) {
            statement.setString(1, csv.toString());
            try (var rows = statement.executeQuery()) {
                final Map<String, ColumnAnalysisResultDto> columns = mapper.resultSetToConstraintResult(rows);
                assertEquals(Set.of("id", "YES"), columns.keySet());
                assertColumn(columns.get("id"), "id", ColumnTypeDto.BIGINT, true, false);
                assertColumn(columns.get("YES"), "YES", ColumnTypeDto.BIGINT, true, false);
            }
        }
    }

    private void assertColumn(ColumnAnalysisResultDto column, String name, ColumnTypeDto datatype,
                              boolean nullAllowed, boolean primaryKey) {
        assertAll(name,
                () -> assertEquals(name, column.getName()),
                () -> assertEquals(datatype, column.getDatatype()),
                () -> assertEquals(nullAllowed, column.getNullAllowed()),
                () -> assertEquals(primaryKey, column.getPrimaryKey()));
    }
}
