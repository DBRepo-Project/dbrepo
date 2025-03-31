package at.tuwien.service;

import at.ac.tuwien.ifs.dbrepo.core.api.ExportResourceDto;
import at.tuwien.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class StorageServiceIntegrationTest extends BaseTest {

    @Autowired
    private StorageService storageService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Config s3Config;

    @Autowired
    private SparkSession sparkSession;

    public static Stream<Arguments> loadDataset_arguments() {
        return Stream.of(
                Arguments.arguments("withHeader", ",", true, 4968),
                Arguments.arguments("withoutHeader", ",", false, 4969)
        );
    }

    @Container
    private static final MinIOContainer minIOContainer = new MinIOContainer(MINIO_IMAGE);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.endpoints.storageService", minIOContainer::getS3URL);
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* s3 */
        if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(s3Config.getS3Bucket()))) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(s3Config.getS3Bucket())
                    .build());
        }
        if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(s3Config.getS3Bucket()))) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(s3Config.getS3Bucket())
                    .build());
        }
    }

    @Test
    public void getObject_succeeds() throws StorageUnavailableException, StorageNotFoundException {

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        final InputStream response = storageService.getObject(s3Config.getS3Bucket(), "s3key");
        assertNotNull(response);
    }

    @Test
    public void getObject_notFound_fails() {

        /* test */
        assertThrows(StorageNotFoundException.class, () -> {
            storageService.getObject(s3Config.getS3Bucket(), "i_do_not_exist");
        });
    }

    @Test
    public void getObject_bucket_fails() {

        /* test */
        assertThrows(StorageUnavailableException.class, () -> {
            storageService.getObject("i_do_not_exist", "s3key");
        });
    }

    @Test
    public void getBytes_succeeds() throws StorageUnavailableException, StorageNotFoundException {

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        final byte[] response = storageService.getBytes(s3Config.getS3Bucket(), "s3key");
        assertNotNull(response);
    }

    @Test
    public void getBytes_simple_succeeds() throws StorageUnavailableException, StorageNotFoundException {

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        final byte[] response = storageService.getBytes("s3key");
        assertNotNull(response);
    }

    @Test
    public void getBytes_notFound_fails() {

        /* test */
        assertThrows(StorageNotFoundException.class, () -> {
            storageService.getBytes(s3Config.getS3Bucket(), "i_do_not_exist");
        });
    }

    @Test
    public void getResource_succeeds() throws StorageUnavailableException, StorageNotFoundException {

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        final ExportResourceDto response = storageService.getResource(s3Config.getS3Bucket(), "s3key");
        assertEquals("s3key", response.getFilename());
        assertNotNull(response.getResource());
    }

    @Test
    public void getResource_notFound_fails() {

        /* test */
        assertThrows(StorageNotFoundException.class, () -> {
            storageService.getBytes(s3Config.getS3Bucket(), "i_do_not_exist");
        });
    }

    @Test
    public void transformDataset_succeeds() throws StorageUnavailableException, IOException {
        final Dataset<Row> request = sparkSession.read()
                .option("header", "true")
                .csv("./src/test/resources/csv/keyboard.csv");
        final List<String> fileLines = FileUtils.readLines(new File("./src/test/resources/csv/keyboard.csv"), Charset.defaultCharset());

        /* test */
        final ExportResourceDto response = storageService.transformDataset(request);
        assertNotNull(response.getFilename());
        assertEquals("dataset.csv", response.getFilename());
        assertNotNull(response.getResource());
        final List<String> lines = new BufferedReader(new InputStreamReader(response.getResource().getInputStream()))
                .lines()
                .parallel()
                .toList();
        final String[] columNames = lines.get(0).split(",");
        assertEquals(10, columNames.length);
        assertArrayEquals(new String[]{"Shift key time", "Esc key time", "Ctrl key time", "Alt key time", "User ID", "Test date", "Gender", "Right hand", "Birth year", "Computer skill level"}, columNames);
        assertEquals(4969, lines.size());
        for (int i = 0; i < 4969; i++) {
            assertEquals(fileLines.get(i), lines.get(i));
        }
    }

    @Test
    public void transformDataset_empty_succeeds() throws StorageUnavailableException, IOException {
        final Dataset<Row> request = sparkSession.emptyDataFrame();

        /* test */
        final ExportResourceDto response = storageService.transformDataset(request);
        assertNotNull(response.getFilename());
        assertEquals("dataset.csv", response.getFilename());
        assertNotNull(response.getResource());
        final List<String> lines = new BufferedReader(new InputStreamReader(response.getResource().getInputStream()))
                .lines()
                .parallel()
                .toList();
        assertEquals(1, lines.size());
        assertEquals("", lines.get(0));
    }

    @ParameterizedTest
    @Disabled("cannot fix")
    @MethodSource("loadDataset_arguments")
    public void generic_loadDataset(String name, String separator, Boolean withHeader, Integer expectedRows)
            throws StorageUnavailableException, StorageNotFoundException, IOException, MalformedException, TableMalformedException {
        final List<String> fileLines = FileUtils.readLines(new File("./src/test/resources/csv/keyboard.csv"), Charset.defaultCharset());
        final String[] requestHeaders = new String[]{"Shift key time", "Esc key time", "Ctrl key time", "Alt key time", "User ID", "Test date", "Gender", "Right hand", "Birth year", "Computer skill level"};

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/keyboard.csv")));

        /* test */
        final Dataset<Row> response = storageService.loadDataset(Arrays.asList(requestHeaders), "s3key", separator, withHeader);
        final List<Map<String, String>> rows = datasetToRows(response);
        assertEquals(expectedRows, rows.size());
        for (int i = 0; i < expectedRows; i++) {
            assertEquals(fileLines.get(i + (withHeader ? 1 : 0)), String.join(",", rows.get(i).values()));
        }
    }

    public List<Map<String, String>> datasetToRows(Dataset<Row> data) {
        return data.collectAsList()
                .stream()
                .map(row -> {
                    final Map<String, String> map = new LinkedHashMap<>();
                    for (int i = 0; i < data.columns().length; i++) {
                        final String value = row.getString(i);
                        map.put(data.columns()[i], row.get(i) != null ? value : "");
                    }
                    return map;
                })
                .toList();
    }

}
