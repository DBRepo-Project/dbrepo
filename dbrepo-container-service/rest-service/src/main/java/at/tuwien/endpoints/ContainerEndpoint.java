package at.tuwien.endpoints;

import at.tuwien.api.container.*;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.ContainerMapper;
import at.tuwien.service.UserService;
import at.tuwien.service.impl.ContainerServiceImpl;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    private final UserService userService;
    private final ContainerMapper containerMapper;
    private final ContainerServiceImpl containerService;

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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List containers",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerBriefDto[].class))}),
    })
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new container",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerBriefDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Docker client failed to connect",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container image or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container image or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Container name already exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found container",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Docker client failed to connect",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container image could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to the container failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
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
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Modify some container", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Modified state of container successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerBriefDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Modification of container state is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Container is already started/stopped",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ContainerBriefDto> modify(@NotNull @PathVariable("id") Long containerId,
                                                    @Valid @RequestBody ContainerChangeDto changeDto,
                                                    @NotNull Principal principal)
            throws ContainerNotFoundException, ContainerAlreadyRunningException, ContainerAlreadyStoppedException,
            UserNotFoundException, NotAllowedException {
        log.debug("endpoint modify container, containerId={}, changeDto={}, principal={}", containerId, changeDto, principal);
        final User user = userService.findByUsername(principal.getName());
        final Container container = containerService.find(containerId);
        if (!container.getOwner().getId().equals(user.getId()) && user.getRoles().stream().noneMatch(r -> r.name().equals("ROLE_DEVELOPER"))) {
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
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Delete some container", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted container successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "Container is still running",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "Container is already removed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull Principal principal) throws ContainerNotFoundException,
            ContainerStillRunningException, ContainerAlreadyRemovedException {
        log.debug("endpoint delete container, containerId={}, principal={}", containerId, principal);
        containerService.remove(containerId);
        return ResponseEntity.accepted()
                .build();
    }

}
