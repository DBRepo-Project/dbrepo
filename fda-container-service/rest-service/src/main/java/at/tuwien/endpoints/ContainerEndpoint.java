package at.tuwien.endpoints;

import at.tuwien.api.container.*;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.ContainerMapper;
import at.tuwien.service.ContainerService;
import at.tuwien.service.UserService;
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
import java.util.Optional;
import java.util.stream.Collectors;


@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/container")
public class ContainerEndpoint {

    private final UserService userService;
    private final ContainerMapper containerMapper;
    private final ContainerService containerService;

    @Autowired
    public ContainerEndpoint(UserService userService, ContainerServiceImpl containerService,
                             ContainerMapper containerMapper) {
        this.userService = userService;
        this.containerMapper = containerMapper;
        this.containerService = containerService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Find all containers")
    public ResponseEntity<List<ContainerBriefDto>> findAll(Principal principal,
                                                           @RequestParam(required = false) Integer limit) {
        log.debug("endpoint find all containers, principal={}, limit={}", principal, limit);
        final List<Container> containers = containerService.getAll(limit);
        final List<com.github.dockerjava.api.model.Container> list = containerService.list();
        final List<ContainerBriefDto> dtos = containers.stream()
                .map(containerMapper::containerToDatabaseContainerBriefDto)
                .peek(container -> {
                    final Optional<com.github.dockerjava.api.model.Container> optional = list.stream()
                            .filter(c -> c.getId().equals(container.getHash()))
                            .findFirst();
                    optional.ifPresent(value -> {
                        final String state = value.getState();
                        log.trace("container {} has status {}", container.getId(), state);
                        container.setRunning(state.equals("running"));
                    });
                })
                .collect(Collectors.toList());
        log.trace("find all containers resulted in containers {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('create-container')")
    @Operation(summary = "Create container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ContainerBriefDto> create(@Valid @RequestBody ContainerCreateRequestDto data,
                                                    @NotNull Principal principal)
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
    @PreAuthorize("hasAuthority('modify-container-state')")
    @Operation(summary = "Modify some container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ContainerBriefDto> modify(@NotNull @PathVariable("id") Long containerId,
                                                    @Valid @RequestBody ContainerChangeDto changeDto,
                                                    @NotNull Principal principal)
            throws ContainerNotFoundException, ContainerAlreadyRunningException, ContainerAlreadyStoppedException,
            UserNotFoundException, NotAllowedException, DockerClientException {
        log.debug("endpoint modify container, containerId={}, changeDto={}, principal={}", containerId, changeDto, principal);
        final User user = userService.findByUsername(principal.getName());
        final Container container = containerService.find(containerId);
        if (!(container.getCreator().getId().equals(user.getId()))) {
            log.error("Failed to modify container because it is not owned '{}' by the current user {} or is not developer", container.getCreator().getUsername(), user.getUsername());
            throw new NotAllowedException("Failed to modify container because it is not owned by the current user or is not developer");
        }
        final Container entity;
        if (changeDto.getAction().equals(ContainerActionTypeDto.START)) {
            log.trace("request attempts to start the container");
            entity = containerService.start(containerId);
        } else {
            log.trace("request attempts to stop the container");
            entity = containerService.stop(containerId);
        }
        final ContainerBriefDto dto = containerMapper.containerToDatabaseContainerBriefDto(entity);
        log.trace("modify container resulted in container {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Timed(value = "container.delete", description = "Time needed to delete the container")
    @PreAuthorize("hasAuthority('delete-container')")
    @Operation(summary = "Delete some container", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull Principal principal) throws ContainerNotFoundException,
            ContainerStillRunningException, ContainerAlreadyRemovedException, DockerClientException {
        log.debug("endpoint delete container, containerId={}, principal={}", containerId, principal);
        containerService.remove(containerId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}
