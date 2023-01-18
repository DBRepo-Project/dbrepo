package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.impl.ImageServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PersistenceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private ImageServiceImpl imageService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void beforeEach() {
        userRepository.save(USER_1);
        final ContainerImage tmp = ContainerImage.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .hash(IMAGE_1_HASH)
                .jdbcMethod(IMAGE_1_JDBC)
                .dialect(IMAGE_1_DIALECT)
                .driverClass(IMAGE_1_DRIVER)
                .compiled(IMAGE_1_BUILT)
                .size(IMAGE_1_SIZE)
                .environment(IMAGE_1_ENV)
                .defaultPort(IMAGE_1_PORT)
                .build();
        imageRepository.save(tmp);
    }

    @Test
    public void delete_notExists_fails() {

        /* test */
        assertThrows(UnexpectedRollbackException.class, () -> {
            imageService.delete(IMAGE_2_ID);
        });
    }

}
