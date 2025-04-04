package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.CreateContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.ContainerService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping(path = "/api/container")
public class ContainerEndpoint extends AbstractEndpoint {

    private final MetadataMapper metadataMapper;
    private final ContainerService containerService;

    @Autowired
    public ContainerEndpoint(ContainerService containerService, MetadataMapper metadataMapper) {
        this.metadataMapper = metadataMapper;
        this.containerService = containerService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_container_findall")
    @Operation(summary = "List containers",
            description = "List all containers in the metadata database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List containers",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ContainerBriefDto.class)))}),
    })
    public ResponseEntity<List<ContainerBriefDto>> findAll(@RequestParam(required = false) Integer limit) {
        log.debug("endpoint find all containers, limit={}", limit);
        return ResponseEntity.ok()
                .body(containerService.getAll(limit)
                        .stream()
                        .map(metadataMapper::containerToContainerBriefDto)
                        .collect(Collectors.toList()));
    }

    @PostMapping
    @Transactional
    @Observed(name = "dbrepo_container_create")
    @PreAuthorize("hasAuthority('create-container')")
    @Operation(summary = "Create container",
            description = "Creates a container in the metadata database. Requires role `create-container`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new container",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Container payload malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Create container not permitted, need authority `create-container`",
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
    public ResponseEntity<ContainerDto> create(@Valid @RequestBody CreateContainerDto data)
            throws ImageNotFoundException, ContainerAlreadyExistsException {
        log.debug("endpoint create container, data={}", data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.containerToContainerDto(containerService.create(data)));
    }

    @GetMapping("/{containerId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_container_find")
    @Operation(summary = "Find container",
            description = "Finds a container in the metadata database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found container",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container image could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ContainerDto> findById(@NotNull @PathVariable("containerId") UUID containerId,
                                                 Principal principal)
            throws ContainerNotFoundException {
        log.debug("endpoint find container, containerId={}", containerId);
        final Container container = containerService.find(containerId);
        final HttpHeaders headers = new HttpHeaders();
        if (isSystem(principal)) {
            log.trace("attach privileged credential information");
            headers.set("X-Host", container.getHost());
            headers.set("X-Port", "" + container.getPort());
            headers.set("X-Username", container.getPrivilegedUsername());
            headers.set("X-Password", container.getPrivilegedPassword());
            headers.set("X-Jdbc-Method", container.getImage().getJdbcMethod());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password X-Jdbc-Method X-Host X-Port");
        }
        return ResponseEntity.ok()
                .headers(headers)
                .body(metadataMapper.containerToContainerDto(container));
    }

    @DeleteMapping("/{containerId}")
    @Transactional
    @Observed(name = "dbrepo_container_delete")
    @PreAuthorize("hasAuthority('delete-container')")
    @Operation(summary = "Delete container",
            description = "Deletes a container in the metadata database. Requires role `delete-container`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted container"),
            @ApiResponse(responseCode = "403",
                    description = "Create container not permitted, need authority `delete-container`",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("containerId") UUID containerId)
            throws ContainerNotFoundException {
        log.debug("endpoint delete container, containerId={}", containerId);
        containerService.remove(containerService.find(containerId));
        return ResponseEntity.accepted()
                .build();
    }

}
