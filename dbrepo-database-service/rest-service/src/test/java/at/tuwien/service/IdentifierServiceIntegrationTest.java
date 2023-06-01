package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.exception.AmqpException;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.impl.RabbitMqServiceImpl;
import at.tuwien.utils.AmqpUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class IdentifierServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private IdentifierService identifierService;

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_1);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_2_ID);
        assertEquals(0, response.size());
    }


}
