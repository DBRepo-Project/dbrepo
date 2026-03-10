package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.S3Util;
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
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

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
    public void putObject_succeeds() throws IOException, StorageObjectExistsException {
        final byte[] request = FileUtils.readFileToByteArray(new File("./src/test/resources/csv/keyboard.csv"));

        /* test */
        storageService.putObject("keyboard.csv", request);
        final GetObjectResponse response = s3Client.getObject(GetObjectRequest.builder()
                        .key("keyboard.csv")
                        .bucket(s3Config.getS3Bucket())
                        .build())
                .response();
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
    public void getResource_notFound_fails() {

        /* test */
        assertThrows(StorageNotFoundException.class, () -> {
            storageService.getBytes(s3Config.getS3Bucket(), "i_do_not_exist");
        });
    }

}
