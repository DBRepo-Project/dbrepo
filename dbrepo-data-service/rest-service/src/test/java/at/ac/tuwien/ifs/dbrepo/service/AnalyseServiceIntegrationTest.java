package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.PostgresContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.mapper.DuckDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.AnalyseServiceDuckDbImpl;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import at.ac.tuwien.ifs.dbrepo.utils.S3Util;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class AnalyseServiceIntegrationTest extends BaseTest {

    @Autowired
    private AnalyseServiceDuckDbImpl analyseService;

    @Autowired
    private DuckDbMapper duckDbMapper;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Config s3Config;

    public static Stream<Arguments> query_arguments() {
        return Stream.of(
                Arguments.arguments("simpleQuery", "SELECT id, date, location, mintemp, rainfall FROM weather_aus", 5, new HashMap<>() {{
                    put("id", ColumnTypeDto.BIGINT);
                    put("date", ColumnTypeDto.DATE);
                    put("location", ColumnTypeDto.VARCHAR);
                    put("mintemp", ColumnTypeDto.DOUBLE);
                    put("rainfall", ColumnTypeDto.DOUBLE);
                }}),
                Arguments.arguments("sortQuery", "SELECT id, date, location, mintemp, rainfall FROM weather_aus ORDER BY id ASC", 5, new HashMap<>() {{
                    put("id", ColumnTypeDto.BIGINT);
                    put("date", ColumnTypeDto.DATE);
                    put("location", ColumnTypeDto.VARCHAR);
                    put("mintemp", ColumnTypeDto.DOUBLE);
                    put("rainfall", ColumnTypeDto.DOUBLE);
                }}),
                Arguments.arguments("filterQuery", "SELECT id, date, location, mintemp, rainfall FROM weather_aus WHERE (id > 2)", 5, new HashMap<>() {{
                    put("id", ColumnTypeDto.BIGINT);
                    put("date", ColumnTypeDto.DATE);
                    put("location", ColumnTypeDto.VARCHAR);
                    put("mintemp", ColumnTypeDto.DOUBLE);
                    put("rainfall", ColumnTypeDto.DOUBLE);
                }}),
                Arguments.arguments("mixedFormattedQuery", "SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` WHERE (`id` > 2) ORDER BY `id` ASC", 5, new HashMap<>() {{
                    put("id", ColumnTypeDto.BIGINT);
                    put("date", ColumnTypeDto.DATE);
                    put("location", ColumnTypeDto.VARCHAR);
                    put("mintemp", ColumnTypeDto.DOUBLE);
                    put("rainfall", ColumnTypeDto.DOUBLE);
                }})
        );
    }

    @Container
    private static PostgresContainerConfig.CustomPostgresContainer postgresContainer = PostgresContainerConfig.getContainer();

    @Container
    private static final MinIOContainer minIOContainer = new MinIOContainer(MINIO_IMAGE);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.spark.hadoop.fs.s3a.endpoint", minIOContainer::getS3URL);
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException, InterruptedException {
        /* s3 */
        S3Util.cleanBucket(s3Client, s3Config);
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
    }

    @Test
    public void determineDataTypes_succeeds() throws DatabaseUnavailableException, ColumnNotFoundException {

        /* test */
        final Map<String, ColumnAnalysisResultDto> response = analyseService.determineDataTypes(DATABASE_1_CACHE, QUERY_1_DTO);
        assertEquals(5, response.size());
        final ColumnAnalysisResultDto id = response.get("id");
        assertEquals("id", id.getName());
        assertEquals(ColumnTypeDto.BIGINT, id.getDatatype());
        final ColumnAnalysisResultDto date = response.get("date");
        assertEquals("date", date.getName());
        assertEquals(ColumnTypeDto.DATE, date.getDatatype());
        final ColumnAnalysisResultDto location = response.get("location");
        assertEquals("location", location.getName());
        assertEquals(ColumnTypeDto.VARCHAR, location.getDatatype());
        final ColumnAnalysisResultDto mintemp = response.get("mintemp");
        assertEquals("mintemp", mintemp.getName());
        assertEquals(ColumnTypeDto.DOUBLE, mintemp.getDatatype());
        final ColumnAnalysisResultDto rainfall = response.get("rainfall");
        assertEquals("rainfall", rainfall.getName());
        assertEquals(ColumnTypeDto.DOUBLE, rainfall.getDatatype());
    }

    @ParameterizedTest
    @MethodSource("query_arguments")
    public void determineDataTypes_complex_succeeds(String name, String statement, Integer expectedRows,
                                                    Map<String, ColumnTypeDto> expectedAnalysis)
            throws DatabaseUnavailableException, ColumnNotFoundException {

        /* test */
        final Map<String, ColumnAnalysisResultDto> response = analyseService.determineDataTypes(DATABASE_1_CACHE, duckDbMapper.queryToRawDescribeQuery(statement));
        assertEquals(expectedRows, response.size());
        for (Map.Entry<String, ColumnTypeDto> row : expectedAnalysis.entrySet()) {
            final ColumnAnalysisResultDto column = response.get(row.getKey());
            assertEquals(row.getKey(), column.getName());
            assertEquals(row.getValue(), column.getDatatype());
        }
    }

    @Test
    public void determineS3CsvDataTypes_succeeds() throws DatabaseUnavailableException, ColumnNotFoundException,
            StorageNotFoundException, ImageInvalidException, AnalyseDataTypesException {

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("weather_aus.csv")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        final SchemaAnalysisResultDto response = analyseService.determineS3CsvDataTypes(IMAGE_1_CACHE, "weather_aus.csv");
    }

}
