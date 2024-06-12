package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.maintenance.BannerMessageBriefDto;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.exception.MessageNotFoundException;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.BannerMessageService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/message")
public class MessageEndpoint {

    private final MetadataMapper metadataMapper;
    private final BannerMessageService bannerMessageService;

    @Autowired
    public MessageEndpoint(MetadataMapper metadataMapper, BannerMessageService bannerMessageService) {
        this.metadataMapper = metadataMapper;
        this.bannerMessageService = bannerMessageService;
    }

    @GetMapping
    @Observed(name = "dbrepo_maintenance_findall")
    @Operation(summary = "List messages",
            description = "Lists messages known to the metadata database. Messages can be filtered be filtered with the optional `active` parameter. If set to *true*, only active messages (that is, messages whose end time has not been reached) will be returned. Otherwise only inactive messages are returned. If not set, active and inactive messages are returned.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List messages",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = BannerMessageDto.class)))}),
    })
    public ResponseEntity<List<BannerMessageDto>> list(@RequestParam(required = false) Boolean active) {
        log.debug("endpoint list messages, active={}", active);
        List<BannerMessageDto> dtos;
        if (active != null && active) {
            dtos = bannerMessageService.getActive()
                    .stream()
                    .map(metadataMapper::bannerMessageToBannerMessageDto)
                    .toList();
        } else {
            dtos = bannerMessageService.findAll()
                    .stream()
                    .map(metadataMapper::bannerMessageToBannerMessageDto)
                    .toList();
        }
        log.info("List messages resulted in {} message(s)", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/message/{messageId}")
    @Observed(name = "dbrepo_maintenance_find")
    @Operation(summary = "Find message",
            description = "Finds a message with id in the metadata database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get messages",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BannerMessageDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find message",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<BannerMessageDto> find(@NotNull @PathVariable("messageId") Long messageId)
            throws MessageNotFoundException {
        log.debug("endpoint find one maintenance message, messageId={}", messageId);
        final BannerMessageDto dto = metadataMapper.bannerMessageToBannerMessageDto(bannerMessageService.find(messageId));
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Observed(name = "dbrepo_maintenance_create")
    @PreAuthorize("hasAuthority('create-maintenance-message')")
    @Operation(summary = "Create message",
            description = "Creates a message in the metadata database. Requires role `create-maintenance-message`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created message",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BannerMessageBriefDto.class))}),
    })
    public ResponseEntity<BannerMessageDto> create(@Valid @RequestBody BannerMessageCreateDto data) {
        log.debug("endpoint create maintenance message, data={}", data);
        final BannerMessageDto dto = metadataMapper.bannerMessageToBannerMessageDto(bannerMessageService.create(data));
        log.trace("create maintenance message results in dto {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping("/{messageId}")
    @Observed(name = "dbrepo_maintenance_update")
    @PreAuthorize("hasAuthority('update-maintenance-message')")
    @Operation(summary = "Update message",
            description = "Updates a message with id. Requires role `update-maintenance-message`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated message",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BannerMessageBriefDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find message",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<BannerMessageDto> update(@NotNull @PathVariable("messageId") Long messageId,
                                                   @Valid @RequestBody BannerMessageUpdateDto data)
            throws MessageNotFoundException {
        log.debug("endpoint update maintenance message, messageId={}, data={}", messageId, data);
        final BannerMessage message = bannerMessageService.find(messageId);
        final BannerMessageDto dto = metadataMapper.bannerMessageToBannerMessageDto(bannerMessageService.update(message, data));
        log.trace("update maintenance message results in dto {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @DeleteMapping("/{messageId}")
    @Observed(name = "dbrepo_maintenance_delete")
    @PreAuthorize("hasAuthority('delete-maintenance-message')")
    @Operation(summary = "Delete message",
            description = "Deletes a message with id. Requires role `delete-maintenance-message`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted message",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find message",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("messageId") Long messageId)
            throws MessageNotFoundException {
        log.debug("endpoint delete maintenance message, messageId={}", messageId);
        final BannerMessage message = bannerMessageService.find(messageId);
        bannerMessageService.delete(message);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}
