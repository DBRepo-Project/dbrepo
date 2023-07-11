package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.ContainerRepository;
import at.tuwien.repository.mdb.ImageRepository;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ContainerServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private RealmRepository realmRepository;

    @BeforeEach
    public void beforeEach() {
        /* mock data */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        imageRepository.save(IMAGE_1_SIMPLE);
    }

    @Test
    public void create_succeeds() throws ImageNotFoundException, ContainerAlreadyExistsException, UserNotFoundException {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .imageId(IMAGE_1_ID)
                .name(CONTAINER_1_NAME)
                .build();

        /* test */
        final Container container = containerService.create(request, USER_1_PRINCIPAL);
        assertEquals(CONTAINER_1_NAME, container.getName());
    }

    @Test
    public void create_conflictingNames_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .imageId(IMAGE_1_ID)
                .name(CONTAINER_1_NAME)
                .build();

        /* mock */
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerAlreadyExistsException.class, () -> {
            containerService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void remove_alreadyRemoved_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void create_notFound_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_3_NAME)
                .imageId(IMAGE_2_ID)
                .build();

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            containerService.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findById_notFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.find(CONTAINER_1_ID);
        });
    }

    @Test
    public void getAll_succeeds() {

        /* mock */
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2_SIMPLE);

        /* test */
        final List<Container> response = containerService.getAll(null);
        assertEquals(2, response.size());
    }

    @Test
    public void getAll_limit_succeeds() {

        /* mock */
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2_SIMPLE);

        /* test */
        final List<Container> response = containerService.getAll(1);
        assertEquals(1, response.size());
    }

    @Test
    public void remove_succeeds() throws ContainerStillRunningException, ContainerNotFoundException, ContainerAlreadyRemovedException {

        /* mock */
        containerRepository.save(CONTAINER_1);

        /* test */
        containerService.remove(CONTAINER_1_ID);
    }

    @Test
    public void remove_notFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }
}
