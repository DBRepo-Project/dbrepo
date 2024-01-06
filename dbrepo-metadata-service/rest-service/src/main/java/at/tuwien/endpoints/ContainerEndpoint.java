package at.tuwien.endpoints;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.ContainerAlreadyExistsException;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.mapper.ContainerMapper;
import at.tuwien.service.ContainerService;
import at.tuwien.utils.PrincipalUtil;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;


@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/container")
public class ContainerEndpoint {

    private final ContainerMapper containerMapper;
    private final ContainerService containerService;

    @Autowired
    public ContainerEndpoint(ContainerService containerService, ContainerMapper containerMapper) {
        this.containerMapper = containerMapper;
        this.containerService = containerService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_container_findall")
    @Operation(summary = "Find all containers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List containers",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ContainerBriefDto.class)))}),
    })
    public ResponseEntity<List<ContainerBriefDto>> findAll(Principal principal,
                                                           @RequestParam(required = false) Integer limit) {
        log.debug("endpoint find all containers, limit={}, {}", limit, PrincipalUtil.formatForDebug(principal));
        final List<Container> containers = containerService.getAll(limit);
        final List<ContainerBriefDto> dtos = containers.stream()
                .map(containerMapper::containerToDatabaseContainerBriefDto)
                .collect(Collectors.toList());
        log.trace("find all containers resulted in containers {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @PostMapping
    @Transactional
    @Observed(name = "dbr_container_create")
    @PreAuthorize("hasAuthority('create-container')")
    @Operation(summary = "Create container", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new container",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerBriefDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Failed to connect",
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
            throws ImageNotFoundException, ContainerAlreadyExistsException {
        log.debug("endpoint create container, data={}, {}", data, PrincipalUtil.formatForDebug(principal));
        final Container container = containerService.create(data, principal);
        final ContainerBriefDto dto = containerMapper.containerToDatabaseContainerBriefDto(container);
        log.trace("create container resulted in container {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Observed(name = "dbr_container_find")
    @Operation(summary = "Find some container")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found container",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Failed to connect",
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
    public ResponseEntity<ContainerDto> findById(@NotNull @PathVariable("id") Long containerId)
            throws  ContainerNotFoundException {
        log.debug("endpoint find container, id={}", containerId);
        final Container container = containerService.find(containerId);
        final ContainerDto dto = containerMapper.containerToContainerDto(container);
        log.trace("find container resulted in container {}", dto);
        return ResponseEntity.ok()
                .body(dto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Observed(name = "dbr_container_delete")
    @PreAuthorize("hasAuthority('delete-container')")
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
                                    @NotNull Principal principal) throws ContainerNotFoundException {
        log.debug("endpoint delete container, containerId={}, {}", containerId, PrincipalUtil.formatForDebug(principal));
        containerService.remove(containerId);
        return ResponseEntity.accepted()
                .build();
    }

}
