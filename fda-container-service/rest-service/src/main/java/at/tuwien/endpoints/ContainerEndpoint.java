package at.tuwien.endpoints;

import at.tuwien.api.container.*;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.mapper.ContainerMapper;
import at.tuwien.service.impl.ContainerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;


@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/container")
public class ContainerEndpoint {

    private final ContainerServiceImpl containerService;
    private final ContainerMapper containerMapper;

    @Autowired
    public ContainerEndpoint(ContainerServiceImpl containerService, ContainerMapper containerMapper) {
        this.containerMapper = containerMapper;
        this.containerService = containerService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Find all containers")
    public ResponseEntity<List<ContainerBriefDto>> findAll(Principal principal) {
        final List<Container> containers = containerService.getAll();
        return ResponseEntity.ok()
                .body(containers.stream()
                        .map(containerMapper::containerToDatabaseContainerBriefDto)
                        .collect(Collectors.toList()));
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Create container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ContainerBriefDto> create(@Valid @RequestBody ContainerCreateRequestDto data,
                                                    Principal principal)
            throws ImageNotFoundException, DockerClientException, ContainerAlreadyExistsException,
            UserNotFoundException {
        final Container container = containerService.create(data, principal);
        final ContainerBriefDto response = containerMapper.containerToDatabaseContainerBriefDto(container);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find some container")
    public ResponseEntity<ContainerDto> findById(@NotNull @PathVariable("id") Long containerId) throws DockerClientException,
            ContainerNotFoundException, ContainerNotRunningException {
        final Container container = containerService.inspect(containerId);
        return ResponseEntity.ok()
                .body(containerMapper.containerToContainerDto(container));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Modify some container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ContainerBriefDto> modify(@NotNull @PathVariable("id") Long containerId,
                                                    @Valid @RequestBody ContainerChangeDto changeDto)
            throws ContainerNotFoundException, DockerClientException {
        final Container container;
        if (changeDto.getAction().equals(ContainerActionTypeDto.START)) {
            container = containerService.start(containerId);
        } else {
            container = containerService.stop(containerId);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(containerMapper.containerToDatabaseContainerBriefDto(container));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER') and hasPermission(#containerId, 'DELETE_CONTAINER')")
    @Operation(summary = "Delete some container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId) throws ContainerNotFoundException,
            DockerClientException, ContainerStillRunningException {
        containerService.remove(containerId);
        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

}
