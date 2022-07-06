package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.endpoints.DocumentEndpoint;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.exception.UserInvenioTokenException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.AuthenticationServiceGateway;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DocumentEndpointUnitTest extends BaseUnitTest {

    @Autowired
    private DocumentEndpoint documentEndpoint;

    @MockBean
    private AuthenticationServiceGateway authenticationServiceGateway;


    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"ROLE_RESEARCHER"})
    public void create_succeed() throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException {
        final CreateDraftDto request = DOCUMENT_1_CREATE_DRAFT;
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(authenticationServiceGateway.validate(anyString()))
                .thenReturn(USER_1_DETAILS);

        /* test */
        final ResponseEntity<RecordDto> response = documentEndpoint.create(request, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

}
