package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.entities.user.Realm;
import at.tuwien.exception.RealmNotFoundException;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.sdb.UserIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class RealmServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private UserIdxRepository userIdxRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private RealmService realmService;

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
    }

    @Test
    public void find_succeeds() throws RealmNotFoundException {

        /* test */
        final Realm response = realmService.find(REALM_DBREPO_NAME);
        assertNotNull(response);
        assertEquals(REALM_DBREPO_ID, response.getId());
        assertEquals(REALM_DBREPO_NAME, response.getName());
        assertEquals(REALM_DBREPO_ENABLED, response.getEnabled());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(RealmNotFoundException.class, () -> {
            realmService.find("shadow");
        });
    }
}
