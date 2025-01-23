package at.tuwien.service;

import at.tuwien.api.container.CreateContainerDto;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.ContainerRepository;
import at.tuwien.repository.ImageRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ContainerServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private ImageRepository imageRepository;

    @Autowired
    private ContainerService containerService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void create_succeeds() throws ContainerAlreadyExistsException, ImageNotFoundException {
        final CreateContainerDto request = CreateContainerDto.builder()
                .imageId(IMAGE_1_ID)
                .name(CONTAINER_1_NAME)
                .build();

        /* mock */
        when(containerRepository.findByInternalName(CONTAINER_1_NAME))
                .thenReturn(Optional.empty());
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.of(IMAGE_1));
        when(containerRepository.save(any(Container.class)))
                .thenReturn(CONTAINER_1);

        /* test */
        final Container container = containerService.create(request);
        assertEquals(CONTAINER_1_NAME, container.getName());
    }

    @Test
    public void create_containerExists_fails() {
        final CreateContainerDto request = CreateContainerDto.builder()
                .imageId(IMAGE_1_ID)
                .name(CONTAINER_1_NAME)
                .build();

        /* mock */
        when(containerRepository.findByInternalName(CONTAINER_1_INTERNALNAME))
                .thenReturn(Optional.of(CONTAINER_1));

        /* test */
        assertThrows(ContainerAlreadyExistsException.class, () -> {
            containerService.create(request);
        });
    }

    @Test
    public void create_imageNotFound_fails() {
        final CreateContainerDto request = CreateContainerDto.builder()
                .name(CONTAINER_3_NAME)
                .imageId(9999L)
                .build();

        /* mock */
        when(containerRepository.findByInternalName(CONTAINER_1_NAME))
                .thenReturn(Optional.empty());
        when(imageRepository.findById(IMAGE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            containerService.create(request);
        });
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            find_generic(CONTAINER_1_ID, null);
        });
    }

    @Test
    public void find_succeeds() throws ContainerNotFoundException {

        /* test */
        find_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    public void getAll_succeeds() {
        final List<Container> containers = List.of(CONTAINER_1, CONTAINER_2);

        /* mock */
        when(containerRepository.findByOrderByCreatedDesc(Pageable.ofSize(2)))
                .thenReturn(containers);

        /* test */
        getAll_generic(2, containers);
    }

    @Test
    public void getAll_limit_succeeds() {
        final List<Container> containers = List.of(CONTAINER_1);

        /* mock */
        when(containerRepository.findByOrderByCreatedDesc(Pageable.ofSize(1)))
                .thenReturn(containers);

        /* test */
        getAll_generic(1, containers);
    }

    @Test
    public void remove_succeeds() throws ContainerNotFoundException {

        /* test */
        containerService.remove(CONTAINER_1);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void getAll_generic(Integer limit, List<Container> containers) {

        /* mock */
        if (limit != null) {
            when(containerRepository.findAll(any(Sort.class)))
                    .thenReturn(containers);
        } else {
            when(containerRepository.findAll(any(PageRequest.class)).toList())
                    .thenReturn(containers);
        }

        /* test */
        final List<Container> response = containerService.getAll(limit);
        assertEquals(limit, response.size());
    }

    protected void find_generic(Long containerId, Container container) throws ContainerNotFoundException {

        /* mock */
        if (container != null) {
            when(containerRepository.findById(containerId))
                    .thenReturn(Optional.of(container));
        } else {
            when(containerRepository.findById(anyLong()))
                    .thenReturn(Optional.empty());
        }

        /* test */
        final Container response = containerService.find(containerId);
        assertEquals(CONTAINER_1_ID, response.getId());
    }
}
