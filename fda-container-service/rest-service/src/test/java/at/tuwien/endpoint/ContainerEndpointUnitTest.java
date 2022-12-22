package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.ContainerStateDto;
import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ContainerEndpoint;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.impl.ContainerServiceImpl;
import at.tuwien.service.impl.ImageServiceImpl;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class ContainerEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private ContainerServiceImpl containerService;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Test
    public void findById_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* mock */
        when(containerService.inspect(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.findById(CONTAINER_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final ContainerDto dto = response.getBody();
        assertEquals(ContainerStateDto.RUNNING, dto.getState());
        assertEquals(CONTAINER_1_ID, dto.getId());
        assertEquals(CONTAINER_1_NAME, dto.getName());
        assertEquals(CONTAINER_1_INTERNALNAME, dto.getInternalName());
        assertEquals(CONTAINER_1_IP, dto.getIpAddress());
    }

}
