package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.document.file.FileStartDto;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.DraftDto;
import at.tuwien.exception.DraftRecordCreateException;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;

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
    public void start_succeeds() throws DraftRecordCreateException {
        final CreateDraftDto request = DOCUMENT_1_CREATE_DRAFT;
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        final DraftDto document = documentService.create(request, principal);

        /* test */
        final FileStartDto response = fileService.start(document.getId(), principal);
    }

}
