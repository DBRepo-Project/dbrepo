package at.tuwien.endpoints;

import at.tuwien.api.container.*;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.mapper.ContainerMapper;
import at.tuwien.service.impl.ContainerServiceImpl;
import io.micrometer.core.annotation.Timed;
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
        log.debug("endpoint find all containers, principal={}", principal);
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
        log.debug("endpoint create container, data={}, principal={}", data, principal);
        final Container container = containerService.create(data, principal);
        final ContainerBriefDto dto = containerMapper.containerToDatabaseContainerBriefDto(container);
        log.trace("create container resulted in container {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find some container")
    public ResponseEntity<ContainerDto> findById(@NotNull @PathVariable("id") Long containerId) throws DockerClientException,
            ContainerNotFoundException, ContainerNotRunningException {
        log.debug("endpoint find container, id={}", containerId);
        final Container container = containerService.inspect(containerId);
        final ContainerDto dto = containerMapper.containerToContainerDto(container);
        dto.setState(ContainerStateDto.RUNNING);
        log.trace("find container resulted in container {}", dto);
        return ResponseEntity.ok()
                .body(dto);
    }

    @PutMapping("/{id}")
    @Transactional
    @Timed(value = "container.modify", description = "Time needed to modify the container state")
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Modify some container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ContainerBriefDto> modify(@NotNull @PathVariable("id") Long containerId,
                                                    @Valid @RequestBody ContainerChangeDto changeDto)
            throws ContainerNotFoundException, ContainerAlreadyRunningException, ContainerAlreadyStoppedException {
        log.debug("endpoint modify container, containerId={}, changeDto={}", containerId, changeDto);
        final Container container;
        if (changeDto.getAction().equals(ContainerActionTypeDto.START)) {
            log.trace("request attempts to start the container");
            container = containerService.start(containerId);
        } else {
            log.trace("request attempts to stop the container");
            container = containerService.stop(containerId);
        }
        final ContainerBriefDto dto = containerMapper.containerToDatabaseContainerBriefDto(container);
        log.trace("modify container resulted in container {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Timed(value = "container.delete", description = "Time needed to delete the container")
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Delete some container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull Principal principal) throws ContainerNotFoundException,
            ContainerStillRunningException, ContainerAlreadyRemovedException {
        log.debug("endpoint delete container, containerId={}, principal={}", containerId, principal);
        containerService.remove(containerId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}
