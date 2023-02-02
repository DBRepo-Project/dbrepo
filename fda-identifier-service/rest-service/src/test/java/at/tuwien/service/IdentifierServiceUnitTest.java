package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.api.identifier.VisibilityTypeDto;
import at.tuwien.config.IndexInitializer;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.IdentifierIdxRepository;
import at.tuwien.repository.jpa.IdentifierRepository;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierServiceUnitTest extends BaseUnitTest {

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private IdentifierIdxRepository identifierIdxRepository;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private UserService userService;

    @Autowired
    private IdentifierService identifierService;

    @Test
    public void findAll_succeeds() {

        /* mock */
        when(identifierRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID, null);
        assertEquals(1, response.size());
        assertEquals(IDENTIFIER_1, response.get(0));
    }

    @Test
    public void find_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final Identifier response = identifierService.find(IDENTIFIER_1_ID);
        assertEquals(IDENTIFIER_1, response);
    }

    @Test
    public void find_fails() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(IDENTIFIER_1_ID);
        });
    }

    @Test
    public void update_notFound_fails() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.empty());
        when(identifierRepository.save(IDENTIFIER_1))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO);
        });
    }

    @Test
    public void create_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException {
        final IdentifierCreateDto request = IdentifierCreateDto.builder()
                .cid(CONTAINER_1_ID)
                .dbid(DATABASE_1_ID)
                .type(IdentifierTypeDto.SUBSET)
                .qid(IDENTIFIER_1_QUERY_ID)
                .description(IDENTIFIER_1_DESCRIPTION)
                .title(IDENTIFIER_1_TITLE)
                .doi(IDENTIFIER_1_DOI)
                .visibility(VisibilityTypeDto.EVERYONE)
                .creators(List.of(CREATOR_1_CREATE_DTO, CREATOR_2_CREATE_DTO))
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(databaseService.find(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.ok(QUERY_1_DTO));
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(identifierRepository.save(any(Identifier.class)))
                .thenReturn(IDENTIFIER_1);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_1_DTO);


        /* test */
        identifierService.create(request, principal, bearer);
    }

    @Test
    public void delete_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));
        doNothing()
                .when(identifierRepository)
                .delete(IDENTIFIER_1);
        doNothing()
                .when(identifierIdxRepository)
                .deleteById(IDENTIFIER_1_ID);

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
    }

    @Test
    public void delete_notFound_fails() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.empty());
        doNothing()
                .when(identifierRepository)
                .delete(IDENTIFIER_1);

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.delete(IDENTIFIER_1_ID);
        });
    }

}
