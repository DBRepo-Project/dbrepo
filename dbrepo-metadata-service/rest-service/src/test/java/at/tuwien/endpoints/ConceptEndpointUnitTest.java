package at.tuwien.endpoints;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.service.ConceptService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ConceptEndpointUnitTest extends AbstractUnitTest {

    @MockBean
    private ConceptService conceptService;

    @Autowired
    private ConceptEndpoint conceptEndpoint;

    @Test
    @WithAnonymousUser
    public void findAllConcepts_anonymous_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void findAllConcepts_noRole_succeeds() {

        /* test */
        findAll_generic();
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAll_generic() {

        /* mock */
        when(conceptService.findAll())
                .thenReturn(List.of(CONCEPT_1, CONCEPT_2));

        /* test */
        final ResponseEntity<List<ConceptDto>> response = conceptEndpoint.findAll();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<ConceptDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }
   
}
