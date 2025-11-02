package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.UnitDto;
import at.ac.tuwien.ifs.dbrepo.service.UnitService;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
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
public class UnitEndpointUnitTest extends BaseTest {

    @MockitoBean
    private UnitService unitService;

    @Autowired
    private UnitEndpoint unitEndpoint;

    @Test
    @WithAnonymousUser
    public void findAllUnits_anonymous_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAllUnits_noRole_succeeds() {

        /* test */
        findAll_generic();
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAll_generic() {

        /* mock */
        when(unitService.findAll())
                .thenReturn(List.of(UNIT_1, UNIT_2));

        /* test */
        final ResponseEntity<List<UnitDto>> response = unitEndpoint.findAll();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<UnitDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }
}
