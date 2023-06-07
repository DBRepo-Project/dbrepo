package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ImageEndpoint;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.any;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private RealmRepository realmRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ImageEndpoint imageEndpoint;

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* networks */
        DockerConfig.createAllNetworks();
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_2_SIMPLE);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-image"})
    public void create_succeeds() throws UserNotFoundException, ImageAlreadyExistsException, DockerClientException,
            ImageNotFoundException, ImageInvalidException {


        /* test */
        imageEndpoint.create(IMAGE_1_CREATE_DTO, USER_2_PRINCIPAL);
    }

}
