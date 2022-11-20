package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.ContainerAlreadyExistsException;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ContainerServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerService containerService;

    @BeforeEach
    public void beforeEach() {
        /* mock data */
        userRepository.save(USER_1);
        final ContainerImage tmp = ContainerImage.builder()
                .id(IMAGE_1_ID)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .hash(IMAGE_1_HASH)
                .jdbcMethod("mariadb")
                .dialect("org.hibernate.dialect.MariaDBDialect")
                .driverClass("org.mariadb.jdbc.Driver")
                .compiled(IMAGE_1_BUILT)
                .size(IMAGE_1_SIZE)
                .environment(IMAGE_1_ENV)
                .defaultPort(IMAGE_1_PORT)
                .build();
        imageRepository.save(tmp);
        containerRepository.save(CONTAINER_1);
    }

    @Test
    public void create_nameExists_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* test */
        assertThrows(ContainerAlreadyExistsException.class, () -> {
            containerService.create(request, principal);
        });
    }
}
