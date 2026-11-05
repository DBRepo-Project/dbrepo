package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.impl.StorageServiceS3Impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceS3UnitTest {

    private static final GetObjectRequest REQUEST = GetObjectRequest.builder()
            .bucket("uploads").key("image.png").build();

    private StorageServiceS3Impl service;
    private ResponseInputStream<GetObjectResponse> stream;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final S3Client client = mock(S3Client.class);
        final S3Config config = mock(S3Config.class);
        when(config.getS3Bucket()).thenReturn("uploads");
        stream = mock(ResponseInputStream.class);
        when(client.getObject(REQUEST)).thenReturn(stream);
        service = new StorageServiceS3Impl(config, client);
    }

    @Test
    void getBytes_closesStreamAfterSuccessfulRead() throws Exception {
        final byte[] content = "image content".getBytes(StandardCharsets.UTF_8);
        when(stream.readAllBytes()).thenReturn(content);

        assertArrayEquals(content, service.getBytes("image.png"));

        verify(stream).close();
    }

    @Test
    void getBytes_closesStreamWhenReadingFails() throws Exception {
        final IOException failure = new IOException("Read interrupted");
        when(stream.readAllBytes()).thenThrow(failure);

        final StorageNotFoundException error = assertThrows(StorageNotFoundException.class,
                () -> service.getBytes("uploads", "image.png"));

        assertSame(failure, error.getCause());
        verify(stream).close();
    }
}
