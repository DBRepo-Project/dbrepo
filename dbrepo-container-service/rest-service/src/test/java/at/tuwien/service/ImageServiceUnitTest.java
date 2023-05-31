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

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

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

    @MockBean
    private ImageServiceImpl imageService;

    @MockBean
    private ImageRepository imageRepository;

    @Test
    public void getAll_succeeds() {

        /* mock */
        when(imageRepository.findAll())
                .thenReturn(List.of(IMAGE_1));
        when(imageService.getAll())
                .thenCallRealMethod();

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
        when(imageService.find(IMAGE_1_ID))
                .thenCallRealMethod();

        /* test */
        final ContainerImage response = imageService.find(IMAGE_1_ID);
        assertEquals(IMAGE_1_REPOSITORY, response.getRepository());
        assertEquals(IMAGE_1_TAG, response.getTag());
    }

    @Test
    public void getById_notFound_fails() throws ImageNotFoundException {

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.empty());
        when(imageService.find(IMAGE_1_ID))
                .thenCallRealMethod();

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageService.find(IMAGE_1_ID);
        });
    }

    @Test
    public void create_duplicate_fails() throws UserNotFoundException, ImageAlreadyExistsException,
            DockerClientException, ImageNotFoundException {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(imageRepository.save(any(ContainerImage.class)))
                .thenThrow(ConstraintViolationException.class);
        when(imageService.create(request, USER_1_PRINCIPAL))
                .thenCallRealMethod();

        /* test */
        assertThrows(ImageAlreadyExistsException.class, () -> {
            imageService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void update_succeeds() throws ImageNotFoundException {
        final ImageChangeDto request = ImageChangeDto.builder()
                .registry(IMAGE_1_REGISTRY)
                .environment(IMAGE_1_ENV_DTO)
                .defaultPort(IMAGE_1_PORT)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));
        when(imageRepository.save(any()))
                .thenReturn(IMAGE_1);
        doNothing()
                .when(imageService)
                .pull(IMAGE_1_REGISTRY, IMAGE_1_REPOSITORY, IMAGE_1_TAG);
        when(imageService.update(IMAGE_1_ID, request))
                .thenCallRealMethod();

        /* test */
        final ContainerImage response = imageService.update(IMAGE_1_ID, request);
        assertEquals(IMAGE_1_REPOSITORY, response.getRepository());
        assertEquals(IMAGE_1_TAG, response.getTag());
    }

    @Test
    public void update_port_succeeds() throws ImageNotFoundException {
        final ImageChangeDto request = ImageChangeDto.builder()
                .registry(IMAGE_1_REGISTRY)
                .environment(IMAGE_1_ENV_DTO)
                .defaultPort(9999)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));
        when(imageRepository.save(any()))
                .thenReturn(IMAGE_1);
        doNothing()
                .when(imageService)
                .pull(IMAGE_1_REGISTRY, IMAGE_1_REPOSITORY, IMAGE_1_TAG);
        when(imageService.update(IMAGE_1_ID, request))
                .thenCallRealMethod();

        /* test */
        final ContainerImage response = imageService.update(IMAGE_1_ID, request);
        assertEquals(IMAGE_1_REPOSITORY, response.getRepository());
        assertEquals(IMAGE_1_TAG, response.getTag());
    }

    @Test
    public void update_notFound_fails() throws ImageNotFoundException {
        final ImageChangeDto request = ImageChangeDto.builder()
                .environment(IMAGE_1_ENV_DTO)
                .defaultPort(IMAGE_1_PORT)
                .build();

        /* mock */
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.empty());
        when(imageService.update(IMAGE_1_ID, request))
                .thenCallRealMethod();

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageService.update(IMAGE_1_ID, request);
        });
    }

    @Test
    public void delete_succeeds() throws ImageNotFoundException {

        /* mock */
        when(imageRepository.existsById(IMAGE_1_ID))
                .thenReturn(true);
        doNothing()
                .when(imageRepository)
                .deleteById(IMAGE_1_ID);
        doCallRealMethod()
                .when(imageService)
                .delete(IMAGE_1_ID);

        /* test */
        imageService.delete(IMAGE_1_ID);
    }

    @Test
    public void delete_notFound_fails() throws ImageNotFoundException {

        /* mock */
        when(imageRepository.existsById(IMAGE_1_ID))
                .thenReturn(false);
        doThrow(EntityNotFoundException.class)
                .when(imageRepository)
                .deleteById(IMAGE_1_ID);
        doCallRealMethod()
                .when(imageService)
                .delete(IMAGE_1_ID);

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
