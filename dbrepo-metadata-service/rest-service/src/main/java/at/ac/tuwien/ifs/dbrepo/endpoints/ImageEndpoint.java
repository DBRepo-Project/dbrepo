package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageChangeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageInvalidException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.ImageService;
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
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping(path = "/api/image")
public class ImageEndpoint extends AbstractEndpoint {

    private final ImageService imageService;
    private final MetadataMapper metadataMapper;

    @Autowired
    public ImageEndpoint(ImageService imageService, MetadataMapper metadataMapper) {
        this.imageService = imageService;
        this.metadataMapper = metadataMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_image_findall")
    @Operation(summary = "List images",
            description = "Lists all container images known to the metadata database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List images",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ImageBriefDto.class)))}),
    })
    public ResponseEntity<List<ImageBriefDto>> findAll() {
        log.debug("endpoint find all images");
        final List<ContainerImage> containers = imageService.getAll();
        return ResponseEntity.ok()
                .body(containers.stream()
                        .map(metadataMapper::containerImageToImageBriefDto)
                        .toList());
    }

    @PostMapping
    @Transactional
    @Observed(name = "dbrepo_image_create")
    @PreAuthorize("hasAuthority('create-image')")
    @Operation(summary = "Create image",
            description = "Creates a container image in the metadata database. Requires role `create-image`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
            @ApiResponse(responseCode = "409",
                    description = "Image already exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ImageDto> create(@Valid @RequestBody ImageCreateDto data,
                                           @NotNull Principal principal) throws ImageAlreadyExistsException,
            ImageInvalidException {
        log.debug("endpoint create image, data={}, principal.name={}", data, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.containerImageToImageDto(
                        imageService.create(data, principal)));
    }

    @GetMapping("/{imageId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_image_find")
    @Operation(summary = "Find image",
            description = "Finds a container image in the metadata database.")
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
    public ResponseEntity<ImageDto> findById(@NotNull @PathVariable("imageId") UUID imageId) throws ImageNotFoundException {
        log.debug("endpoint find image, imageId={}", imageId);
        return ResponseEntity.ok()
                .body(metadataMapper.containerImageToImageDto(imageService.find(imageId)));
    }

    @PutMapping("/{imageId}")
    @Transactional
    @Observed(name = "dbrepo_image_update")
    @PreAuthorize("hasAuthority('modify-image')")
    @Operation(summary = "Update image",
            description = "Updates container image in the metadata database. Requires role `modify-image`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<ImageDto> update(@NotNull @PathVariable("imageId") UUID imageId,
                                           @RequestBody @Valid ImageChangeDto changeDto)
            throws ImageNotFoundException {
        log.debug("endpoint update image, imageId={}, changeDto={}", imageId, changeDto);
        return ResponseEntity.accepted()
                .body(metadataMapper.containerImageToImageDto(
                        imageService.update(imageService.find(imageId), changeDto)));
    }

    @DeleteMapping("/{imageId}")
    @Transactional
    @Observed(name = "dbrepo_image_delete")
    @PreAuthorize("hasAuthority('delete-image')")
    @Operation(summary = "Delete image",
            description = "Deletes a container image in the metadata database. Requires role `delete-image`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<Void> delete(@NotNull @PathVariable("imageId") UUID imageId) throws ImageNotFoundException {
        log.debug("endpoint delete image, imageId={}", imageId);
        imageService.delete(imageService.find(imageId));
        return ResponseEntity.accepted()
                .build();
    }

}
