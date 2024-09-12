package at.tuwien.service;

import at.tuwien.exception.ImageInvalidException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.repository.ImageRepository;
import at.tuwien.service.impl.ImageServiceImpl;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private ImageRepository imageRepository;

    @Autowired
    private ImageService imageService;

    @Test
    public void getAll_succeeds() {

        /* mock */
        when(imageRepository.findAll())
                .thenReturn(List.of(IMAGE_1));

        /* test */
        final List<ContainerImage> response = imageService.getAll();
        assertEquals(1, response.size());
        assertEquals(IMAGE_1_NAME, response.get(0).getName());
        assertEquals(IMAGE_1_VERSION, response.get(0).getVersion());
    }

    @Test
    public void getById_succeeds() throws ImageNotFoundException {

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));

        /* test */
        final ContainerImage response = imageService.find(IMAGE_1_ID);
        assertEquals(IMAGE_1_NAME, response.getName());
        assertEquals(IMAGE_1_VERSION, response.getVersion());
    }

    @Test
    public void getById_notFound_fails() {

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageService.find(IMAGE_1_ID);
        });
    }

    @Test
    public void create_duplicate_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version(IMAGE_1_VERSION)
                .defaultPort(IMAGE_1_PORT)
                .build();

        /* mock */
        when(imageRepository.findByNameAndVersion(IMAGE_1_NAME, IMAGE_1_VERSION))
                .thenReturn(Optional.of(IMAGE_1));

        /* test */
        assertThrows(ImageAlreadyExistsException.class, () -> {
            imageService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void create_multipleDefaults_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version("10.5")
                .defaultPort(IMAGE_1_PORT)
                .isDefault(true)
                .build();

        /* mock */
        when(imageRepository.findByNameAndVersion(IMAGE_1_NAME, IMAGE_1_VERSION))
                .thenReturn(Optional.empty());
        when(imageRepository.findByIsDefault(true))
                .thenReturn(Optional.of(IMAGE_1));

        /* test */
        assertThrows(ImageInvalidException.class, () -> {
            imageService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void update_succeeds() {
        final ImageServiceImpl mockImageService = mock(ImageServiceImpl.class);
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_PORT)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));
        when(imageRepository.save(any()))
                .thenReturn(IMAGE_1);
        when(mockImageService.update(IMAGE_1, request))
                .thenReturn(CONTAINER_1_IMAGE);

        /* test */
        final ContainerImage response = mockImageService.update(IMAGE_1, request);
        assertEquals(IMAGE_1_NAME, response.getName());
        assertEquals(IMAGE_1_VERSION, response.getVersion());
    }

    @Test
    public void update_port_succeeds() {
        final ImageServiceImpl mockImageService = mock(ImageServiceImpl.class);
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(9999)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));
        when(imageRepository.save(any()))
                .thenReturn(IMAGE_1);
        when(mockImageService.update(IMAGE_1, request))
                .thenReturn(CONTAINER_1_IMAGE);

        /* test */
        final ContainerImage response = mockImageService.update(IMAGE_1, request);
        assertEquals(IMAGE_1_NAME, response.getName());
        assertEquals(IMAGE_1_VERSION, response.getVersion());
    }

    @Test
    public void toString_omitSecrets_succeeds() {

        /* test */
        final String response = IMAGE_1.toString();
        assertFalse(response.contains("MARIADB_PASSWORD"));
        assertFalse(response.contains("MARIADB_ROOT_PASSWORD"));
    }

    @Test
    public void toString_omitSecrets2_succeeds() {

        /* test */
        final String response = CONTAINER_1.toString();
        assertFalse(response.contains("MARIADB_PASSWORD"));
        assertFalse(response.contains("MARIADB_ROOT_PASSWORD"));
    }
}
