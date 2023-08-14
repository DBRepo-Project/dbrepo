package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.user.UserAttribute;
import at.tuwien.exception.UserAttributeNotFoundException;
import at.tuwien.repository.mdb.UserAttributeRepository;
import at.tuwien.repository.mdb.UserRepository;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class UserAttributeServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAttributeRepository userAttributeRepository;

    @Autowired
    private UserAttributeService userAttributeService;

    @BeforeEach
    public void beforeEach() {
        userRepository.save(USER_1_SIMPLE);
        userAttributeRepository.saveAll(USER_1_ATTRIBUTES);
        userRepository.save(USER_2_SIMPLE);
    }

    @Test
    public void find_succeeds() throws UserAttributeNotFoundException {

        /* test */
        final UserAttribute response = userAttributeService.find(USER_1_ID, "theme_dark");
        assertNotNull(response);
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(UserAttributeNotFoundException.class, () -> {
            userAttributeService.find(USER_2_ID, "theme_dark");
        });
    }

    @Test
    public void create_succeeds() {
        final UserAttribute request = UserAttribute.builder()
                .id(UUID.randomUUID())
                .userId(USER_2_ID)
                .name("debug")
                .value("yes")
                .build();

        /* test */
        final UserAttribute response = userAttributeService.create(request);
        assertNotNull(response);
        assertEquals("debug", response.getName());
        assertEquals("yes", response.getValue());
    }

    @Test
    public void update_succeeds() throws UserAttributeNotFoundException {

        /* test */
        final UserAttribute response = userAttributeService.update(USER_1_ID, "affiliation", "NASA");
        assertNotNull(response);
        assertEquals("affiliation", response.getName());
        assertEquals("NASA", response.getValue());
    }

    @Test
    public void update_fails() {

        /* test */
        assertThrows(UserAttributeNotFoundException.class, () -> {
            userAttributeService.update(USER_2_ID, "affiliation", "NASA");
        });
    }
}
