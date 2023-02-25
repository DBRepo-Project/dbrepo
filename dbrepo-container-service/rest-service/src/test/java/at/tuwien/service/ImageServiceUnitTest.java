package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.impl.ImageServiceImpl;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private ImageServiceImpl imageService;

    @MockBean
    private ImageRepository imageRepository;

    @Test
    public void getAll_succeeds() {

        /* mock */
        when(imageRepository.findAll())
                .thenReturn(List.of(IMAGE_1));

        /* test */
        final List<ContainerImage> response = imageService.getAll();
        assertEquals(1, response.size());
        assertEquals(IMAGE_1_REPOSITORY, response.get(0).getRepository());
        assertEquals(IMAGE_1_TAG, response.get(0).getTag());
    }

    @Test
    public void getById_succeeds() throws ImageNotFoundException {

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));

        /* test */
        final ContainerImage response = imageService.find(IMAGE_1_ID);
        assertEquals(IMAGE_1_REPOSITORY, response.getRepository());
        assertEquals(IMAGE_1_TAG, response.getTag());
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
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .environment(IMAGE_1_ENV_DTO)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(imageRepository.save(any(ContainerImage.class)))
                .thenThrow(ConstraintViolationException.class);

        /* test */
        assertThrows(ImageAlreadyExistsException.class, () -> {
            imageService.create(request, principal);
        });
    }

    @Test
    public void update_succeeds() throws ImageNotFoundException {
        final ImageChangeDto request = ImageChangeDto.builder()
                .environment(IMAGE_1_ENV_DTO)
                .defaultPort(IMAGE_1_PORT)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));
        when(imageRepository.save(any()))
                .thenReturn(IMAGE_1);

        /* test */
        final ContainerImage response = imageService.update(IMAGE_1_ID, request);
        assertEquals(IMAGE_1_REPOSITORY, response.getRepository());
        assertEquals(IMAGE_1_TAG, response.getTag());
    }

    @Test
    public void update_port_succeeds() throws ImageNotFoundException {
        final ImageChangeDto request = ImageChangeDto.builder()
                .environment(IMAGE_1_ENV_DTO)
                .defaultPort(9999)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));
        when(imageRepository.save(any()))
                .thenReturn(IMAGE_1);

        /* test */
        final ContainerImage response = imageService.update(IMAGE_1_ID, request);
        assertEquals(IMAGE_1_REPOSITORY, response.getRepository());
        assertEquals(IMAGE_1_TAG, response.getTag());
    }

    @Test
    public void update_notFound_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .environment(IMAGE_1_ENV_DTO)
                .defaultPort(IMAGE_1_PORT)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageService.update(IMAGE_1_ID, request);
        });
    }

    @Test
    public void delete_succeeds() throws ImageNotFoundException {

        /* mock */
        doNothing()
                .when(imageRepository)
                .deleteById(IMAGE_1_ID);

        /* test */
        imageService.delete(IMAGE_1_ID);
    }

    @Test
    public void delete_notFound_fails() {

        /* mock */
        doThrow(EntityNotFoundException.class)
                .when(imageRepository)
                .deleteById(IMAGE_1_ID);

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageService.delete(IMAGE_1_ID);
        });
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
