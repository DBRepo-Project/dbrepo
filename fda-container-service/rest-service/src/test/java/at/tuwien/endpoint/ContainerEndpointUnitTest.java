package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.*;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ContainerEndpoint;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.impl.ContainerServiceImpl;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ContainerEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Autowired
    private ContainerServiceImpl containerService;

    @Test
    public void create_noAuth_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();

        /* mock */
        when(imageRepository.findByRepositoryAndTag(IMAGE_1_REPOSITORY, IMAGE_1_TAG))
                .thenReturn(Optional.of(IMAGE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            containerEndpoint.create(request, null);
        });
    }

    @Test
    @WithUserDetails(USER_4_USERNAME)
    public void create_notRole_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_4_USERNAME);

        /* mock */
        when(imageRepository.findByRepositoryAndTag(IMAGE_1_REPOSITORY, IMAGE_1_TAG))
                .thenReturn(Optional.of(IMAGE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            containerEndpoint.create(request, principal);
        });
    }

    @Test
    @WithUserDetails(USER_2_USERNAME)
    public void create_notResearcher_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);

        /* mock */
        when(imageRepository.findByRepositoryAndTag(IMAGE_1_REPOSITORY, IMAGE_1_TAG))
                .thenReturn(Optional.of(IMAGE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            containerEndpoint.create(request, principal);
        });
    }

    @Test
    @WithUserDetails(USER_3_USERNAME)
    public void create_notResearcher2_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_3_USERNAME);

        /* mock */
        when(imageRepository.findByRepositoryAndTag(IMAGE_1_REPOSITORY, IMAGE_1_TAG))
                .thenReturn(Optional.of(IMAGE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            containerEndpoint.create(request, principal);
        });
    }

    @Test
    @WithUserDetails(USER_1_USERNAME)
    public void create_noImage_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(imageRepository.findByRepositoryAndTag(IMAGE_1_REPOSITORY, IMAGE_1_TAG))
                .thenReturn(Optional.empty());

        assertThrows(ImageNotFoundException.class, () -> {
            containerEndpoint.create(request, principal);
        });
    }

    @Test
    public void findAll_noAuth_fails() {
        /* mock */
        when(imageRepository.findByRepositoryAndTag(IMAGE_1_REPOSITORY, IMAGE_1_TAG))
                .thenReturn(Optional.empty());

        containerEndpoint.findAll(null);
    }

    @Test
    @WithUserDetails(USER_4_USERNAME)
    public void findAll_noRole_succeeds() {
        final Principal principal = new BasicUserPrincipal(USER_4_USERNAME);

        containerEndpoint.findAll(principal);
    }

    @Test
    @WithUserDetails(USER_1_USERNAME)
    public void delete_notDeveloper_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            containerEndpoint.delete(CONTAINER_1_ID);
        });
    }

    @Test
    @WithUserDetails(USER_1_USERNAME)
    public void delete_noRoles_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            containerEndpoint.delete(CONTAINER_1_ID);
        });
    }
}
