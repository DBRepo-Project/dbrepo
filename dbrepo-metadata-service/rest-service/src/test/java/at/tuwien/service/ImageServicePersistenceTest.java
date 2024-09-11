package at.tuwien.service;

import at.tuwien.exception.ImageInvalidException;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.repository.ContainerRepository;
import at.tuwien.repository.ImageRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageServicePersistenceTest extends AbstractUnitTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        imageRepository.save(IMAGE_1);
    }

    @Test
    public void create_succeeds() throws ImageAlreadyExistsException, ImageInvalidException {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version("11.1.4") // new tag
                .registry(IMAGE_1_REGISTRY)
                .jdbcMethod(IMAGE_1_JDBC)
                .dialect(IMAGE_1_DIALECT)
                .driverClass(IMAGE_1_DRIVER)
                .defaultPort(IMAGE_1_PORT)
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
                .defaultPort(IMAGE_1_PORT)
                .driverClass(IMAGE_1_DRIVER)
                .jdbcMethod(IMAGE_1_JDBC)
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
                .registry(IMAGE_1_REGISTRY)
                .defaultPort(IMAGE_1_PORT)
                .driverClass(IMAGE_1_DRIVER)
                .jdbcMethod(IMAGE_1_JDBC)
                .dialect(IMAGE_1_DIALECT)
                .isDefault(true) // <<<<
                .build();

        /* test */
        assertThrows(ImageInvalidException.class, () -> {
            imageService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void delete_hasNoContainer_succeeds()  {

        /* test */
        imageService.delete(IMAGE_1);
        assertTrue(imageRepository.findById(IMAGE_1_ID).isEmpty());
        assertFalse(containerRepository.findById(CONTAINER_1_ID).isPresent()); /* container should NEVER be deletable in the metadata db */
    }

    @Test
    public void delete_noContainer_succeeds() {

        /* test */
        imageService.delete(IMAGE_1);
        assertTrue(imageRepository.findById(IMAGE_1_ID).isEmpty());
    }

}
