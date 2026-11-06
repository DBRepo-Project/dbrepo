package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.config.SeaweedFsContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseModifyImageDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.DatabaseEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static at.ac.tuwien.ifs.dbrepo.core.test.utils.EndpointUtils.jwtPrincipal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class UploadImageIntegrationTest extends BaseTest {
    @Autowired private DatabaseEndpoint endpoint;
    @Autowired private S3Client s3;
    @Autowired private S3Config config;
    @MockitoBean private DatabaseService databases;
    @Container private static final GenericContainer<?> seaweed = SeaweedFsContainerConfig.getContainer();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.endpoints.storageService", () -> SeaweedFsContainerConfig.getEndpoint(seaweed));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-image"})
    void imageReadsOnlyOwnUploadAndPreservesBytes() throws Exception {
        if (s3.listBuckets().buckets().stream().noneMatch(bucket -> bucket.name().equals(config.getS3Bucket()))) {
            s3.createBucket(CreateBucketRequest.builder().bucket(config.getS3Bucket()).build());
        }
        byte[] image = new byte[]{0, -1, 9, 3, 0};
        String own = UploadMetadata.newKey("image.webp");
        String foreign = UploadMetadata.newKey("image.webp");
        String legacy = UploadMetadata.newKey("legacy.webp");
        for (String key : java.util.List.of(own, foreign, legacy)) {
            var metadata = key.equals(legacy) ? java.util.Map.<String, String>of() : UploadMetadata.metadata("image.webp",
                    key.equals(own) ? USER_1_ID.toString() : USER_3_ID.toString());
            s3.putObject(PutObjectRequest.builder().bucket(config.getS3Bucket()).key(key).metadata(metadata).build(),
                    RequestBody.fromBytes(image));
        }
        when(databases.findForUpdateById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(databases.modifyImage(eq(DATABASE_1), any(byte[].class))).thenReturn(DATABASE_1);
        var principal = jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID);
        for (String key : java.util.List.of(foreign, legacy)) {
            assertThrows(StorageNotFoundException.class, () -> endpoint.modifyImage(DATABASE_1_ID,
                    DatabaseModifyImageDto.builder().key(key).build(), principal));
        }
        verify(databases, never()).modifyImage(any(), any());
        endpoint.modifyImage(DATABASE_1_ID, DatabaseModifyImageDto.builder().key(own).build(), principal);
        var bytes = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(databases).modifyImage(eq(DATABASE_1), bytes.capture());
        assertArrayEquals(image, bytes.getValue());
        assertEquals(3, s3.listObjects(builder -> builder.bucket(config.getS3Bucket())).contents().size());
    }
}
