package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.impl.StorageServiceS3Impl;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceS3UnitTest {

    private static final byte[] CONTENT = "temperature,21.5".getBytes(StandardCharsets.UTF_8);
    private static final HeadObjectRequest HEAD = HeadObjectRequest.builder()
            .bucket("uploads").key("sample.csv").build();

    private S3Client client;
    private StorageServiceS3Impl service;

    @BeforeEach
    void setUp() {
        client = mock(S3Client.class);
        final S3Config config = mock(S3Config.class);
        when(config.getS3Bucket()).thenReturn("uploads");
        service = new StorageServiceS3Impl(config, client);
    }

    static Stream<Map<String, String>> matchingHashes() {
        return Stream.of(
                Map.of("sha256", DigestUtils.sha256Hex(CONTENT)),
                Map.of("sha1", DigestUtils.sha1Hex(CONTENT)),
                Map.of("md5", DigestUtils.md5Hex(CONTENT)));
    }

    @ParameterizedTest
    @MethodSource("matchingHashes")
    void duplicateUpload_checksHeadMetadataWithoutDownloadingObject(Map<String, String> metadata) {
        when(client.headObject(HEAD)).thenReturn(HeadObjectResponse.builder().metadata(metadata).build());

        assertThrows(StorageObjectExistsException.class, () -> service.putObject("sample.csv", CONTENT));

        verify(client).headObject(HEAD);
        verifyNoMoreInteractions(client);
    }

    static Stream<S3Exception> missingObjects() {
        final NoSuchKeyException.Builder missing = NoSuchKeyException.builder();
        missing.statusCode(404);
        return Stream.of(missing.build(), s3Failure(404));
    }

    @ParameterizedTest
    @MethodSource("missingObjects")
    void missingObject_uploadsContentAfterHead404(S3Exception missing) throws Exception {
        when(client.headObject(HEAD)).thenThrow(missing);

        service.putObject("sample.csv", CONTENT);

        verifyUpload();
    }

    @Test
    void changedContent_uploadsNewBytesAndHashes() throws Exception {
        when(client.headObject(HEAD)).thenReturn(HeadObjectResponse.builder()
                .metadata(Map.of("sha256", DigestUtils.sha256Hex("old content"))).build());

        service.putObject("sample.csv", CONTENT);

        verifyUpload();
    }

    @Test
    void missingHashMetadata_uploadsContent() throws Exception {
        when(client.headObject(HEAD)).thenReturn(HeadObjectResponse.builder().build());

        service.putObject("sample.csv", CONTENT);

        verifyUpload();
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 500})
    void otherHeadErrors_propagateWithoutUploading(int status) {
        final S3Exception failure = s3Failure(status);
        when(client.headObject(HEAD)).thenThrow(failure);

        assertSame(failure, assertThrows(S3Exception.class, () -> service.putObject("sample.csv", CONTENT)));

        verify(client).headObject(HEAD);
        verifyNoMoreInteractions(client);
    }

    @Test
    void getBytes_closesResponseAfterReading() throws Exception {
        final ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(response);
        when(response.readAllBytes()).thenReturn(CONTENT);

        assertArrayEquals(CONTENT, service.getBytes("sample.csv"));

        verify(response).close();
    }

    @Test
    void getBytes_closesResponseWhenReadingFails() throws Exception {
        final ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(response);
        final IOException failure = new IOException("Interrupted response");
        when(response.readAllBytes()).thenThrow(failure);

        final StorageNotFoundException error = assertThrows(StorageNotFoundException.class,
                () -> service.getBytes("uploads", "sample.csv"));

        assertSame(failure, error.getCause());
        verify(response).close();
    }

    private static S3Exception s3Failure(int status) {
        final S3Exception.Builder failure = S3Exception.builder();
        failure.statusCode(status);
        return (S3Exception) failure.build();
    }

    private void verifyUpload() throws Exception {
        final ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        final ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(client).headObject(HEAD);
        verify(client).putObject(request.capture(), body.capture());
        assertEquals("uploads", request.getValue().bucket());
        assertEquals("sample.csv", request.getValue().key());
        assertEquals(Map.of("sha256", DigestUtils.sha256Hex(CONTENT),
                "sha1", DigestUtils.sha1Hex(CONTENT),
                "md5", DigestUtils.md5Hex(CONTENT)), request.getValue().metadata());
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            assertArrayEquals(CONTENT, stream.readAllBytes());
        }
        verifyNoMoreInteractions(client);
    }
}
