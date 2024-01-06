package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.ContainerRepository;
import at.tuwien.repository.mdb.ImageRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class ContainerServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerService containerService;

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1);
    }

    @Test
    public void find_succeeds() throws ContainerNotFoundException {

        containerRepository.save(CONTAINER_1);

        /* test */
        final Container response = containerService.find(CONTAINER_1_ID);
        assertEquals(CONTAINER_1_ID, response.getId());
        assertEquals(CONTAINER_1_NAME, response.getName());
        assertEquals(CONTAINER_1_INTERNALNAME, response.getInternalName());
    }

    @Test
    public void find_fails() {

        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.find(CONTAINER_2_ID);
        });
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
                .imageId(9999L)
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
        containerRepository.save(CONTAINER_2);

        /* test */
        final List<Container> response = containerService.getAll(null);
        assertEquals(2, response.size());
    }

    @Test
    public void getAll_limit_succeeds() {

        /* mock */
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);

        /* test */
        final List<Container> response = containerService.getAll(1);
        assertEquals(1, response.size());
    }

    @Test
    public void remove_succeeds() throws ContainerNotFoundException {

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
