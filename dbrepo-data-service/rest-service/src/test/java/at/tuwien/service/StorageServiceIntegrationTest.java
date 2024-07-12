package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.config.S3Config;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.StorageNotFoundException;
import at.tuwien.exception.StorageUnavailableException;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
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
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class StorageServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private StorageService storageService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Config s3Config;

    @Container
    private static final MinIOContainer minIOContainer = new MinIOContainer("minio/minio:RELEASE.2024-06-06T09-36-42Z");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.endpoints.storageService", minIOContainer::getS3URL);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
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

}
