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
        final var file = new MockMultipartFile("file", "data.csv", "text/csv", content);
        when(storageService.putUpload("data.csv", content, USER_1_ID.toString())).thenReturn("allocated.csv");
        final var response = endpoint.create(file, jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("allocated.csv", response.getHeaders().getFirst("X-S3-Key"));
        assertEquals("X-S3-Key", response.getHeaders().getFirst("Access-Control-Expose-Headers"));
        verify(storageService).putUpload("data.csv", content, USER_1_ID.toString());
    }

    @Test
    @WithAnonymousUser
    void anonymousUploadIsRejected() {
        final var file = new MockMultipartFile("file", "data.csv", "text/csv", new byte[]{1});
        assertThrows(AccessDeniedException.class, () -> endpoint.create(file, null));
        verifyNoInteractions(storageService);
    }
}
