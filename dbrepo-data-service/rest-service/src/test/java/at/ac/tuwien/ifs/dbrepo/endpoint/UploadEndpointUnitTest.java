package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.UploadEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static at.ac.tuwien.ifs.dbrepo.core.test.utils.EndpointUtils.jwtPrincipal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class UploadEndpointUnitTest extends BaseTest {
    @MockitoBean private StorageService storageService;
    @Autowired private UploadEndpoint endpoint;

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "upload-file")
    void uploadReturnsAllocatedKeyAndStableOwner() throws Exception {
        final byte[] content = "id,value\n1,first\n".getBytes();
        final var file = new MockMultipartFile("file", "data.csv", "text/csv", content) {
            @Override public byte[] getBytes() { throw new AssertionError("Upload must not allocate the complete content"); }
        };
        final var temporary = new AtomicReference<Path>();
        when(storageService.putUpload(eq("data.csv"), any(Path.class), eq(USER_1_ID.toString()))).thenAnswer(call -> {
            final Path path = call.getArgument(1);
            temporary.set(path);
            assertArrayEquals(content, Files.readAllBytes(path));
            return "allocated.csv";
        });
        final var response = endpoint.create(file, jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("allocated.csv", response.getHeaders().getFirst("X-S3-Key"));
        assertEquals("X-S3-Key", response.getHeaders().getFirst("Access-Control-Expose-Headers"));
        verify(storageService).putUpload("data.csv", temporary.get(), USER_1_ID.toString());
        assertFalse(Files.exists(temporary.get()));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "upload-file")
    void storageFailureRemovesStagingFile() throws Exception {
        final var file = new MockMultipartFile("file", "data.csv", "text/csv", new byte[]{1});
        when(storageService.putUpload(eq("data.csv"), any(Path.class), eq(USER_1_ID.toString())))
                .thenThrow(new StorageUnavailableException("Synthetic storage failure"));
        assertThrows(StorageUnavailableException.class, () -> endpoint.create(file, jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID)));
        final var path = org.mockito.ArgumentCaptor.forClass(Path.class);
        verify(storageService).putUpload(eq("data.csv"), path.capture(), eq(USER_1_ID.toString()));
        assertFalse(Files.exists(path.getValue()));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "upload-file")
    void partialTransferFailureRemovesStagingFileWithoutCallingStorage() throws Exception {
        final var temporary = new AtomicReference<Path>();
        final var file = new MockMultipartFile("file", "data.csv", "text/csv", new byte[]{1}) {
            @Override public void transferTo(File destination) throws IOException {
                temporary.set(destination.toPath());
                Files.writeString(destination.toPath(), "partial");
                throw new IOException("Synthetic transfer failure");
            }
        };
        assertThrows(StorageUnavailableException.class, () -> endpoint.create(file, jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID)));
        assertFalse(Files.exists(temporary.get()));
        verifyNoInteractions(storageService);
    }

    @Test
    @WithAnonymousUser
    void anonymousUploadIsRejected() {
        final var file = new MockMultipartFile("file", "data.csv", "text/csv", new byte[]{1});
        assertThrows(AccessDeniedException.class, () -> endpoint.create(file, null));
        verifyNoInteractions(storageService);
    }
}
