package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs queries against a real database without a storage service: query results must not be staged in S3.
 */
@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class DataServiceIntegrationTest extends BaseTest {

    @Autowired
    private DataService dataService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    private static final String QUERY = "SELECT * FROM `weather`.`types` ORDER BY `id`";

    private static final List<String> COLUMNS = List.of("id", "c_tinyint", "c_bool", "c_smallint", "c_mediumint",
            "c_int", "c_bigint", "c_ubigint", "c_uint", "c_decimal", "c_float", "c_double", "c_bit1", "c_bit8",
            "c_char", "c_varchar", "c_text", "c_enum", "c_set", "c_date", "c_datetime", "c_datetime6", "c_timestamp",
            "c_time", "c_year", "c_blob", "c_json", "c_uuid");

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        execute("CREATE TABLE types (id INT PRIMARY KEY, c_tinyint TINYINT, c_bool BOOL, c_smallint SMALLINT, " +
                "c_mediumint MEDIUMINT, c_int INT, c_bigint BIGINT, c_ubigint BIGINT UNSIGNED, c_uint INT UNSIGNED, " +
                "c_decimal DECIMAL(30,10), c_float FLOAT, c_double DOUBLE, c_bit1 BIT(1), c_bit8 BIT(8), " +
                "c_char CHAR(5), c_varchar VARCHAR(100), c_text TEXT, c_enum ENUM('a','b'), c_set SET('x','y'), " +
                "c_date DATE, c_datetime DATETIME, c_datetime6 DATETIME(6), c_timestamp TIMESTAMP NULL, c_time TIME, " +
                "c_year YEAR, c_blob BLOB, c_json JSON, c_uuid UUID) WITH SYSTEM VERSIONING",
                "INSERT INTO types VALUES (1, -5, true, 300, 70000, 2147483647, 9223372036854775807, " +
                        "18446744073709551615, 4294967295, 12345678901234567890.0123456789, 1.1, 3.141592653589793, " +
                        "b'1', b'10101010', 'ab', 'he said \"hi\", then\nleft', 'plain text', 'b', 'x,y', " +
                        "'2024-02-29', '2024-02-29 13:45:01', '2024-02-29 13:45:01.123456', '2024-02-29 13:45:01', " +
                        "'13:45:01', 2024, x'DEADBEEF', '{\"k\": [1, 2]}', '123e4567-e89b-12d3-a456-426614174000')",
                "INSERT INTO types (id) VALUES (2)",
                "INSERT INTO types (id, c_decimal, c_double, c_varchar) VALUES (3, 0.5, 1e300, '')");
    }

    @Test
    public void query_json_succeeds() throws Exception {

        /* test */
        final QueryResultStream result = dataService.query(DATABASE_1_CACHE, QUERY);
        assertEquals(COLUMNS, result.getColumns());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        result.writeJson(out);
        assertEquals("[" +
                "{\"id\":1,\"c_tinyint\":-5,\"c_bool\":true,\"c_smallint\":300,\"c_mediumint\":70000," +
                "\"c_int\":2147483647,\"c_bigint\":9223372036854775807,\"c_ubigint\":18446744073709551615," +
                "\"c_uint\":4294967295,\"c_decimal\":12345678901234567890.0123456789,\"c_float\":1.1," +
                "\"c_double\":3.141592653589793,\"c_bit1\":true,\"c_bit8\":170,\"c_char\":\"ab\"," +
                "\"c_varchar\":\"he said \\\"hi\\\", then\\nleft\",\"c_text\":\"plain text\",\"c_enum\":\"b\"," +
                "\"c_set\":\"x,y\",\"c_date\":\"2024-02-29\",\"c_datetime\":\"2024-02-29T13:45:01.000Z\"," +
                "\"c_datetime6\":\"2024-02-29T13:45:01.123Z\",\"c_timestamp\":\"2024-02-29T13:45:01.000Z\"," +
                "\"c_time\":\"13:45:01\",\"c_year\":2024,\"c_blob\":\"3q2+7w==\",\"c_json\":\"{\\\"k\\\": [1, 2]}\"," +
                "\"c_uuid\":\"123e4567-e89b-12d3-a456-426614174000\"}," +
                "{\"id\":2" + nulls(COLUMNS.size() - 1, 1) + "}," +
                "{\"id\":3" + nulls(8, 1) + ",\"c_decimal\":0.5000000000,\"c_float\":null,\"c_double\":1.0E300" +
                nulls(3, 12) + ",\"c_varchar\":\"\"" + nulls(12, 16) + "}" +
                "]", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void query_csv_succeeds() throws Exception {

        /* test */
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataService.query(DATABASE_1_CACHE, QUERY)
                .writeCsv(out);
        final String nl = System.lineSeparator();
        assertEquals(String.join(",", COLUMNS) + nl +
                "1,-5,true,300,70000,2147483647,9223372036854775807,18446744073709551615,4294967295," +
                "12345678901234567890.0123456789,1.1,3.141592653589793,true,170,ab,\"he said \"\"hi\"\", then\nleft\"," +
                "plain text,b,\"x,y\",2024-02-29,2024-02-29T13:45:01.000Z,2024-02-29T13:45:01.123Z," +
                "2024-02-29T13:45:01.000Z,13:45:01,2024,3q2+7w==,\"{\"\"k\"\": [1, 2]}\"," +
                "123e4567-e89b-12d3-a456-426614174000" + nl +
                "2" + ",".repeat(COLUMNS.size() - 1) + nl +
                "3" + ",".repeat(9) + "0.5000000000,,1.0E300" + ",".repeat(COLUMNS.size() - 12) + nl,
                out.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void query_empty_succeeds() throws Exception {

        /* test */
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataService.query(DATABASE_1_CACHE, "SELECT * FROM `weather`.`types` WHERE `id` < 0")
                .writeJson(out);
        assertEquals("[]", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void query_tableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            dataService.query(DATABASE_1_CACHE, "SELECT * FROM `weather`.`i_do_not_exist`");
        });
    }

    @Test
    public void query_malformed_fails() {

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            dataService.query(DATABASE_1_CACHE, "SELECT FROM WHERE");
        });
    }

    @Test
    public void writeCsv_clientDisconnects_closesConnection() throws Exception {
        execute("CREATE TABLE big (id INT PRIMARY KEY, pad VARCHAR(200))",
                "INSERT INTO big SELECT seq, REPEAT('x', 200) FROM seq_1_to_200000");
        final long before = countConnections();

        /* test */
        final QueryResultStream result = dataService.query(DATABASE_1_CACHE, "SELECT * FROM `weather`.`big`");
        assertEquals(before + 1, countConnections());
        final long start = System.currentTimeMillis();
        assertThrows(IOException.class, () -> {
            result.writeCsv(new OutputStream() {
                private long written = 0;

                @Override
                public void write(int b) throws IOException {
                    if (++written > 1024 * 1024) {
                        throw new IOException("Broken pipe");
                    }
                }
            });
        });
        final long duration = System.currentTimeMillis() - start;
        log.info("aborted stream after {} ms", duration);
        for (int i = 0; i < 50 && countConnections() > before; i++) {
            Thread.sleep(100);
        }
        assertEquals(before, countConnections());
    }

    private static String nulls(int count, int offset) {
        final StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + count; i++) {
            sb.append(",\"")
                    .append(COLUMNS.get(i))
                    .append("\":null");
        }
        return sb.toString();
    }

    private static void execute(String... statements) throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static long countConnections() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM information_schema.PROCESSLIST " +
                     "WHERE `DB` = 'weather' AND `ID` <> CONNECTION_ID()")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://127.0.0.1:" + CONTAINER_1_PORT + "/" +
                DATABASE_1_INTERNAL_NAME, CONTAINER_1_PRIVILEGED_USERNAME, CONTAINER_1_PRIVILEGED_PASSWORD);
    }

}
