package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.S3Config;
import at.tuwien.exception.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class StorageServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private S3Config s3Config;

    @Autowired
    private StorageService storageService;

    @Container
    private static MinIOContainer minIOContainer = new MinIOContainer("minio/minio")
            .withUserName("seaweedfsadmin")
            .withPassword("seaweedfsadmin");

    @DynamicPropertySource
    static void openSearchProperties(DynamicPropertyRegistry registry) {
        registry.add("fda.s3.endpoint", () -> minIOContainer.getS3URL());
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        s3Config.makeBuckets(s3Config.getS3ImportBucket());
        s3Config.uploadFile(s3Config.getS3ImportBucket(), "./src/test/resources/csv/testdata.csv", "s3_filekey");
    }

    @Test
    public void deleteStaleFiles_succeeds() throws FileStorageException, InterruptedException {

        /* test */
        Thread.sleep(5000);
        storageService.deleteStaleFiles(s3Config.getS3ImportBucket());
        assertFalse(s3Config.objectExists(s3Config.getS3ImportBucket(), "s3_filekey"));
    }

    @Test
    public void deleteStaleFiles_fails() throws FileStorageException {

        /* test */
        storageService.deleteStaleFiles(s3Config.getS3ImportBucket());
        assertTrue(s3Config.objectExists(s3Config.getS3ImportBucket(), "s3_filekey"));
    }

}
