package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.document.file.FileDto;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class FileServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private FileService fileService;

    @Test
    public void upload_succeeds()
            throws DraftRecordCreateException, IOException, CommitFileUploadException, FileUploadException {
        final CreateDraftDto request = DOCUMENT_2_CREATE_DRAFT;
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final File mockFile = new File("src/test/resources/images/mock.png");

        /* mock */
        final MultipartFile file = new MockMultipartFile(mockFile.getName(), FileUtils.openInputStream(mockFile)
                .readAllBytes());
        final RecordDto document = documentService.create(request, principal);
        assertFalse(document.getIsPublished());

        /* test */
        final FileDto response = fileService.uploadFile(document.getId(), file, principal);
        assertEquals(file.getName(), response.getKey());
    }
    @Test
    public void publish_succeeds()
            throws DraftRecordCreateException {
        final CreateDraftDto request = DOCUMENT_1_CREATE_DRAFT;
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        final RecordDto document = documentService.create(request, principal);

        /* test */
        final RecordDto response = documentService.publish(document.getId(), principal);
        assertTrue(response.getIsPublished());
        assertNotNull(response.getPids().getDoi());
    }

}
