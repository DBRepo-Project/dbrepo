package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DocumentServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private DocumentService documentService;

    @Test
    public void create_succeeds() throws DraftRecordCreateException {
        final CreateDraftDto request = DOCUMENT_1_CREATE_DRAFT;
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */

        /* test */
        final RecordDto response = documentService.create(request, principal);
        assertEquals(DOCUMENT_1_TITLE, response.getMetadata().getTitle());
    }

    @Test
    public void reserve_succeeds() throws DraftRecordCreateException {
        final CreateDraftDto request = DOCUMENT_1_CREATE_DRAFT;
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */

        /* test */
        final RecordDto document = documentService.create(request, principal);
        final RecordDto response = documentService.reserveDoi(document.getId(), principal);
        assertNotNull(response.getPids().getDoi());
    }

}
