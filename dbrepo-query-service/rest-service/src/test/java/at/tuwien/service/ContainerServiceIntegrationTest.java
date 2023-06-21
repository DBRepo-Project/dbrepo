package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.ViewIdxRepository;
import com.rabbitmq.client.Channel;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude= RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ContainerServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private ViewIdxRepository viewIdxRepository;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ContainerService containerService;

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
    }

    @Test
    public void find_succeeds() throws ContainerNotFoundException {

        /* test */
        final Container response = containerService.find(CONTAINER_1_ID);
        assertEquals(CONTAINER_1_ID, response.getId());
        assertEquals(CONTAINER_1_NAME, response.getName());
        assertEquals(CONTAINER_1_INTERNALNAME, response.getInternalName());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.find(CONTAINER_2_ID);
        });
    }

}
