package at.tuwien.endpoints;

import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.mapper.ImageMapper;
import at.tuwien.service.impl.ImageServiceImpl;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/image")
public class ImageEndpoint {

    private final ImageServiceImpl imageService;
    private final ImageMapper imageMapper;

    @Autowired
    public ImageEndpoint(ImageServiceImpl imageService, ImageMapper imageMapper) {
        this.imageService = imageService;
        this.imageMapper = imageMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "image.list", description = "Time needed to list the container images")
    @Operation(summary = "Find all images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List images",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerImage[].class))}),
    })
    public ResponseEntity<List<ImageBriefDto>> findAll(@NotNull Principal principal) {
        log.debug("endpoint find all images, principal={}", principal);
        final List<ContainerImage> containers = imageService.getAll();
        return ResponseEntity.ok()
                .body(containers.stream()
                        .map(imageMapper::containerImageToImageBriefDto)
                        .collect(Collectors.toList()));
    }

    @PostMapping
    @Transactional
    @Timed(value = "image.create", description = "Time needed to create a container image")
    @PreAuthorize("hasAuthority('create-image')")
    @Operation(summary = "Create image", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created image",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImageDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Image specification is invalid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Docker client failed to connect",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Image already exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ImageDto> create(@Valid @RequestBody ImageCreateDto data,
                                           @NotNull Principal principal) throws ImageNotFoundException,
            ImageAlreadyExistsException, DockerClientException, UserNotFoundException, ImageInvalidException {
        log.debug("endpoint create image, data={}, principal={}", data, principal);
        if (data.getDefaultPort() == null) {
            log.error("Failed to create image, default port is null");
            throw new ImageInvalidException("Failed to create image, default port is null");
        }
        final ContainerImage image = imageService.create(data, principal);
        final ImageDto dto = imageMapper.containerImageToImageDto(image);
        log.trace("create image resulted in image {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Timed(value = "image.find", description = "Time needed to find a container image")
    @Operation(summary = "Find some image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found image",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImageDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Image could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ImageDto> findById(@NotNull @PathVariable Long id) throws ImageNotFoundException {
        log.debug("endpoint find image, id={}", id);
        final ContainerImage image = imageService.find(id);
        final ImageDto dto = imageMapper.containerImageToImageDto(image);
        log.trace("find image resulted in image {}", dto);
        return ResponseEntity.ok()
                .body(dto);
    }

    @PutMapping("/{id}")
    @Transactional
    @Timed(value = "image.update", description = "Time needed to update a container image")
    @PreAuthorize("hasAuthority('modify-image')")
    @Operation(summary = "Update some image", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated image successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImageDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Image could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ImageDto> update(@NotNull @PathVariable Long id,
                                           @RequestBody @Valid ImageChangeDto changeDto,
                                           @NotNull Principal principal)
            throws ImageNotFoundException {
        log.debug("endpoint update image, id={}, changeDto={}, principal={}", id, changeDto, principal);
        final ContainerImage image = imageService.update(id, changeDto);
        final ImageDto dto = imageMapper.containerImageToImageDto(image);
        log.trace("update image resulted in image {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Timed(value = "image.delete", description = "Time needed to delete a container image")
    @PreAuthorize("hasAuthority('delete-image')")
    @Operation(summary = "Delete some image", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted image successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Image could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable Long imageId,
                                    @NotNull Principal principal) throws ImageNotFoundException {
        log.debug("endpoint delete image, id={}, principal={}", imageId, principal);
        imageService.find(imageId);
        imageService.delete(imageId);
        return ResponseEntity.accepted()
                .build();
    }

}
