package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessage;
import at.ac.tuwien.ifs.dbrepo.core.exception.MessageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.BannerMessageService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
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
        final List<BannerMessage> messages;
        if (active != null && active) {
            messages = bannerMessageService.getActive();
        } else {
            messages = bannerMessageService.findAll();
        }
        return ResponseEntity.ok(messages.stream()
                .map(metadataMapper::bannerMessageToBannerMessageDto)
                .toList());
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
    public ResponseEntity<BannerMessageDto> find(@NotNull @PathVariable("messageId") UUID messageId)
            throws MessageNotFoundException {
        log.debug("endpoint find one maintenance message, messageId={}", messageId);
        return ResponseEntity.ok(metadataMapper.bannerMessageToBannerMessageDto(
                bannerMessageService.find(messageId)));
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.bannerMessageToBannerMessageDto(
                        bannerMessageService.create(data)));
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
    public ResponseEntity<BannerMessageDto> update(@NotNull @PathVariable("messageId") UUID messageId,
                                                   @Valid @RequestBody BannerMessageUpdateDto data)
            throws MessageNotFoundException {
        log.debug("endpoint update maintenance message, messageId={}, data={}", messageId, data);
        final BannerMessage message = bannerMessageService.find(messageId);
        return ResponseEntity.accepted()
                .body(metadataMapper.bannerMessageToBannerMessageDto(
                        bannerMessageService.update(message, data)));
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
    public ResponseEntity<Void> delete(@NotNull @PathVariable("messageId") UUID messageId)
            throws MessageNotFoundException {
        log.debug("endpoint delete maintenance message, messageId={}", messageId);
        final BannerMessage message = bannerMessageService.find(messageId);
        bannerMessageService.delete(message);
        return ResponseEntity.accepted()
                .build();
    }

}
