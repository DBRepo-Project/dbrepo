package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.config.SeaweedFsContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.S3Util;
import at.ac.tuwien.ifs.dbrepo.service.impl.StorageServiceS3Impl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
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

    @Container
    private static final GenericContainer<?> seaweedfsContainer = SeaweedFsContainerConfig.getContainer();

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.spark.hadoop.fs.s3a.endpoint", () -> SeaweedFsContainerConfig.getEndpoint(seaweedfsContainer));
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException, InterruptedException {
        /* s3 */
        S3Util.cleanBucket(s3Client, s3Config);
    }

    @Test
    public void getObject_succeeds() throws StorageUnavailableException, StorageNotFoundException, IOException {

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        try (InputStream response = storageService.getObject(s3Config.getS3Bucket(), "s3key")) {
            assertNotNull(response);
        }
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
        assertThrows(StorageNotFoundException.class, () -> {
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
    public void putObject_succeeds() throws IOException, StorageObjectExistsException {
        final byte[] request = FileUtils.readFileToByteArray(new File("./src/test/resources/csv/keyboard.csv"));

        /* test */
        storageService.putObject("keyboard.csv", request);
        final HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                .key("keyboard.csv")
                .bucket(s3Config.getS3Bucket())
                .build());
        assertEquals(DigestUtils.sha1Hex(request), response.metadata().get("sha1"));
        assertEquals(DigestUtils.sha256Hex(request), response.metadata().get("sha256"));
        assertEquals(DigestUtils.md5Hex(request), response.metadata().get("md5"));
    }

    @Test
    public void putObject_duplicateSkip_fails() throws IOException, StorageObjectExistsException {
        final byte[] request = FileUtils.readFileToByteArray(new File("./src/test/resources/csv/keyboard.csv"));

        /* mock */
        storageService.putObject("keyboard.csv", request);

        /* test */
        assertThrows(StorageObjectExistsException.class, () -> {
            storageService.putObject("keyboard.csv", request);
        });
    }

    @Test
    public void putObject_repeatedDuplicates_leaveConnectionsAvailable() throws Exception {
        try (S3Client limitedClient = S3Client.builder()
                .endpointOverride(URI.create(s3Config.getS3aEndpoint()))
                .region(Region.of(s3Config.getS3Region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        s3Config.getS3aAccessKey(), s3Config.getS3aSecretKey())))
                .forcePathStyle(true)
                .httpClientBuilder(ApacheHttpClient.builder()
                        .maxConnections(3)
                        .connectionAcquisitionTimeout(Duration.ofMillis(300)))
                .overrideConfiguration(config -> config.retryPolicy(RetryPolicy.none()))
                .build()) {
            final StorageService limitedStorage = new StorageServiceS3Impl(s3Config, limitedClient);
            final byte[] content = new byte[1024 * 1024];
            limitedStorage.putObject("duplicate.csv", content);

            for (int i = 0; i < 30; i++) {
                assertThrows(StorageObjectExistsException.class,
                        () -> limitedStorage.putObject("duplicate.csv", content));
            }

            assertDoesNotThrow(() -> limitedClient.headBucket(request -> request.bucket(s3Config.getS3Bucket())));
            assertArrayEquals(content, limitedStorage.getBytes("duplicate.csv"));
        }
    }

    @Test
    public void getResource_notFound_fails() {

        /* test */
        assertThrows(StorageNotFoundException.class, () -> {
            storageService.getBytes(s3Config.getS3Bucket(), "i_do_not_exist");
        });
    }

}
