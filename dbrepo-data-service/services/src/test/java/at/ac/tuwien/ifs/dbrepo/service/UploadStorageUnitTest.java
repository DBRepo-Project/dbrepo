package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import at.ac.tuwien.ifs.dbrepo.service.impl.StorageServiceS3Impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadStorageUnitTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary;
    private static final String OWNER = "b3468012-3375-4d80-b6b0-58b72f96594f";
    private static final String KEY = "a3468012-3375-4d80-b6b0-58b72f96594f.csv";
    private static final HeadObjectRequest HEAD = HeadObjectRequest.builder().bucket("uploads").key(KEY).build();
    private S3Client client;
    private StorageServiceS3Impl service;
    @BeforeEach void setup() {
        client = mock(S3Client.class);
        S3Config config = mock(S3Config.class);
        when(config.getS3Bucket()).thenReturn("uploads");
        service = new StorageServiceS3Impl(config, client);
    }
    @Test void matchingOwnerUsesOnlyHead() throws Exception {
        when(client.headObject(HEAD)).thenReturn(HeadObjectResponse.builder().metadata(Map.of(UploadMetadata.OWNER, OWNER)).build());
        service.requireUploadOwner(KEY, OWNER);
        verify(client).headObject(HEAD);
        verifyNoMoreInteractions(client);
    }
    @ParameterizedTest @ValueSource(strings = {"other-owner", ""})
    void foreignOrLegacyMetadataIsNotFound(String owner) {
        when(client.headObject(HEAD)).thenReturn(HeadObjectResponse.builder().metadata(owner.isEmpty() ? Map.of() : Map.of(UploadMetadata.OWNER, owner)).build());
        assertThrows(StorageNotFoundException.class, () -> service.requireUploadOwner(KEY, OWNER));
        verify(client).headObject(HEAD);
        verifyNoMoreInteractions(client);
    }
    @Test void legacyKeyCannotBeUsedByOrdinaryCaller() {
        assertThrows(StorageNotFoundException.class, () -> service.requireUploadOwner("original.csv", OWNER));
        assertThrows(StorageNotFoundException.class, () -> service.requireUploadOwner(KEY, null));
        verifyNoInteractions(client);
    }
    @Test void trustedSystemPreservesInternalObjectAccess() throws Exception {
        service.requireUploadOwner("internal-export.csv", UploadMetadata.SYSTEM_OWNER);
        verifyNoInteractions(client);
    }
    @ParameterizedTest @ValueSource(ints = {404, 403, 500})
    void headErrorsAreClassifiedWithoutReading(int status) {
        var error = S3Exception.builder().statusCode(status).build();
        when(client.headObject(HEAD)).thenThrow(error);
        if (status == 404) assertThrows(StorageNotFoundException.class, () -> service.requireUploadOwner(KEY, OWNER));
        else assertThrows(StorageUnavailableException.class, () -> service.requireUploadOwner(KEY, OWNER));
        verify(client).headObject(HEAD);
        verifyNoMoreInteractions(client);
    }
    @Test void transportFailureIsUnavailable() {
        when(client.headObject(HEAD)).thenThrow(SdkClientException.create("unavailable"));
        assertThrows(StorageUnavailableException.class, () -> service.requireUploadOwner(KEY, OWNER));
        verify(client).headObject(HEAD);
        verifyNoMoreInteractions(client);
    }

    @Test void uploadWritesOwnerOriginalNameAndUniqueKeyWithoutReading() throws Exception {
        byte[] bytes = "data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file = java.nio.file.Files.write(temporary.resolve("content"), bytes);
        String first = service.putUpload("O'Brien ä.csv.gz", file, OWNER);
        String second = service.putUpload("O'Brien ä.csv.gz", file, OWNER);
        assertNotEquals(first, second);
        assertTrue(UploadMetadata.isUploadKey(first));
        var request = org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = org.mockito.ArgumentCaptor.forClass(software.amazon.awssdk.core.sync.RequestBody.class);
        verify(client, times(2)).putObject(request.capture(), body.capture());
        assertEquals(OWNER, request.getValue().metadata().get(UploadMetadata.OWNER));
        assertEquals("O'Brien ä.csv.gz", new String(java.util.Base64.getDecoder().decode(
                request.getValue().metadata().get(UploadMetadata.FILENAME)), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(java.util.List.of("*"), request.getValue().overrideConfiguration().orElseThrow().headers().get("If-None-Match"));
        assertEquals(org.apache.commons.codec.digest.DigestUtils.sha256Hex(bytes), request.getValue().metadata().get("sha256"));
        assertEquals(org.apache.commons.codec.digest.DigestUtils.sha1Hex(bytes), request.getValue().metadata().get("sha1"));
        assertEquals(org.apache.commons.codec.digest.DigestUtils.md5Hex(bytes), request.getValue().metadata().get("md5"));
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            assertArrayEquals(bytes, stream.readAllBytes());
        }
        verifyNoMoreInteractions(client);
    }
    @Test void failedUploadDoesNotReturnAKey() throws Exception {
        var file = java.nio.file.Files.write(temporary.resolve("content"), new byte[]{1});
        when(client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenThrow(SdkClientException.create("unavailable"));
        assertThrows(StorageUnavailableException.class, () -> service.putUpload("data.csv", file, OWNER));
    }

    @Test void unreadableContentFailsBeforeCallingS3() {
        assertThrows(StorageUnavailableException.class, () -> service.putUpload("data.csv", temporary.resolve("absent"), OWNER));
        verifyNoInteractions(client);
    }
}
