package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.entities.user.Role;
import at.tuwien.exception.RoleNotFoundException;
import at.tuwien.repository.mdb.RoleRepository;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class RoleServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleService roleService;

    @BeforeEach
    public void beforeEach() {
        userRepository.save(USER_1_SIMPLE);
        roleRepository.save(ROLE_DEFAULT_RESEARCHER_ROLES);
    }

    @Test
    public void find_succeeds() throws RoleNotFoundException {

        /* test */
        final Role response = roleService.find(ROLE_DEFAULT_RESEARCHER_ROLES_NAME);
        assertNotNull(response);
        assertEquals(ROLE_DEFAULT_RESEARCHER_ROLES_ID, response.getId());
        assertEquals(ROLE_DEFAULT_RESEARCHER_ROLES_NAME, response.getName());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(RoleNotFoundException.class, () -> {
            roleService.find("1role2rulethemall");
        });
    }
}
