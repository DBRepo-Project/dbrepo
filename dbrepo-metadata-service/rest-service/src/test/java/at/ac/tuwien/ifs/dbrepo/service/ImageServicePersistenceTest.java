package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageInvalidException;
import at.ac.tuwien.ifs.dbrepo.repository.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.repository.ImageRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageServicePersistenceTest extends BaseTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        imageRepository.save(IMAGE_1);
    }

    @Test
    public void create_succeeds() throws ImageAlreadyExistsException, ImageInvalidException {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version("11.1.4") // new tag
                .registry(IMAGE_1.getRegistry())
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .dialect(IMAGE_1_DIALECT)
                .driverClass(IMAGE_1.getDriverClass())
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .isDefault(false)
                .build();

        /* test */
        imageService.create(request, USER_1_PRINCIPAL);
    }

    @Test
    public void create_duplicate_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version(IMAGE_1_VERSION)
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .driverClass(IMAGE_1.getDriverClass())
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .dialect(IMAGE_1_DIALECT)
                .isDefault(IMAGE_1_IS_DEFAULT)
                .build();

        /* test */
        assertThrows(ImageAlreadyExistsException.class, () -> {
            imageService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void create_multipleDefaultImages_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name("mariadb")
                .version("10.5")
                .registry(IMAGE_1.getRegistry())
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .driverClass(IMAGE_1.getDriverClass())
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .dialect(IMAGE_1_DIALECT)
                .isDefault(true) // <<<<
                .build();

        /* test */
        assertThrows(ImageInvalidException.class, () -> {
            imageService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void delete_hasNoContainer_succeeds() {

        /* test */
        imageService.delete(IMAGE_1);
        assertTrue(imageRepository.findById(CONTAINER_1_ID).isEmpty());
        assertFalse(containerRepository.findById(CONTAINER_1_ID).isPresent()); /* container should NEVER be deletable in the metadata db */
    }

    @Test
    public void delete_noContainer_succeeds() {

        /* test */
        imageService.delete(IMAGE_1);
        assertTrue(imageRepository.findById(CONTAINER_1_ID).isEmpty());
    }

}
