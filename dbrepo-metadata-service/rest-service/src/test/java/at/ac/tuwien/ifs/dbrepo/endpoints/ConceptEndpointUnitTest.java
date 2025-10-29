package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ConceptDto;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.ConceptService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ConceptEndpointUnitTest extends BaseTest {

    @MockitoBean
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
    @WithMockUser(username = USER_4_USERNAME)
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
