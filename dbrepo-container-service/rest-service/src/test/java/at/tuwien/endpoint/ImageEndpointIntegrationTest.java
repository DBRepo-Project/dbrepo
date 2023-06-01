package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.DockerDaemonConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ImageEndpoint;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.RealmRepository;
import at.tuwien.repository.jpa.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

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
