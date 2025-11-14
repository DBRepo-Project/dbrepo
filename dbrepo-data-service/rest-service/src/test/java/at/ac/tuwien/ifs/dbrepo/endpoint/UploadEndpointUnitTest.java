package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.UploadEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UploadEndpointUnitTest extends BaseTest {

    @MockitoBean
    private StorageService storageService;

    @Autowired
    private UploadEndpoint uploadEndpoint;

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"upload-file"})
    public void create_succeeds() throws IOException, StorageUnavailableException, StorageObjectExistsException {
        final String filename = "keyboard.csv";
        final byte[] contents = FileUtils.readFileToByteArray(new File("./src/test/resources/csv/keyboard.csv"));
        final MultipartFile request = new MockMultipartFile(filename, contents);

        /* mock */
        doNothing()
                .when(storageService)
                .putObject(filename, contents);

        /* test */
        final ResponseEntity<Void> response = uploadEndpoint.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"upload-file"})
    public void create_duplicate_succeeds() throws IOException, StorageUnavailableException,
            StorageObjectExistsException {
        final String filename = "keyboard.csv";
        final byte[] contents = FileUtils.readFileToByteArray(new File("./src/test/resources/csv/keyboard.csv"));
        final MultipartFile request = new MockMultipartFile(filename, contents);

        /* mock */
        doThrow(StorageObjectExistsException.class)
                .when(storageService)
                .putObject(anyString(), any(byte[].class));

        /* test */
        final ResponseEntity<Void> response = uploadEndpoint.create(request);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() throws IOException {
        final String filename = "keyboard.csv";
        final byte[] contents = FileUtils.readFileToByteArray(new File("./src/test/resources/csv/keyboard.csv"));
        final MultipartFile request = new MockMultipartFile(filename, contents);

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            uploadEndpoint.create(request);
        });
    }

}
