package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/v1/user")
public class UserEndpoint extends RestEndpoint {

    private final UserService userService;
    private final MetadataMapper metadataMapper;

    @Autowired
    public UserEndpoint(UserService userService, MetadataMapper metadataMapper) {
        this.userService = userService;
        this.metadataMapper = metadataMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_users_list")
    @Operation(summary = "List users",
            description = "Lists users known to the metadata database. Internal users are omitted from the result list. If the optional query parameter `username` is present, the result list can be filtered by matching this exact username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List users",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserBriefDto.class)))}),
            @ApiResponse(responseCode = "403",
                    description = "Listing not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<UserBriefDto>> findAll(@RequestParam(required = false) String username) throws NotAllowedException {
        log.debug("endpoint find all users, username={}", username);
        if (username == null) {
            return ResponseEntity.ok(userService.findAll()
                    .stream()
                    .map(metadataMapper::userDtoToUserBriefDto)
                    .toList());
        }
        log.trace("filter by username: {}", username);
        try {
            return ResponseEntity.ok(List.of(metadataMapper.userDtoToUserBriefDto(userService.findByUsername(username))));
        } catch (UserNotFoundException e) {
            log.trace("filter by username {} failed: return empty list", username);
            return ResponseEntity.ok(List.of());
        }
    }

    @RequestMapping(value = "/{username}", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    @Observed(name = "dbrepo_user_find")
    @Operation(summary = "Get user",
            description = "Gets own user information from the metadata database. Requires authentication. Foreign user information can only be obtained if additional role `find-foreign-user` is present. Finding information about internal users results in a 404 error.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Find user is not permitted",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "User was not found",
                    content = {@Content}),
    })
    public ResponseEntity<UserDto> find(@NotNull @PathVariable("username") String username,
                                        Principal principal) throws NotAllowedException,
            UserNotFoundException {
        log.debug("endpoint find a user, username={}", username);
        /* check */
        final UserDto user = userService.findByUsername(username);
        if (!user.getUsername().equals(AuthUtil.getUsername(principal)) && !AuthUtil.hasRole(principal, "find-foreign-user") && !AuthUtil.isSystem(principal)) {
            log.error("Failed to find user: foreign user");
            throw new NotAllowedException("Failed to find user: foreign user");
        }
        final HttpHeaders headers = new HttpHeaders();
        if (AuthUtil.isSystem(principal)) {
            headers.set("X-Username", user.getUsername());
            headers.set("X-Password", user.getAttributes().getPostgresPassword());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(user);
    }

    @PutMapping("/{username}")
    @Transactional
    @PreAuthorize("hasAuthority('modify-user-information')")
    @Observed(name = "dbrepo_user_modify")
    @Operation(summary = "Update user",
            description = "Updates user with given username. Requires role `modify-user-information`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user information",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Modify user query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to modify user metadata",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to modify user at auth service",
                    content = {@Content}),
    })
    public ResponseEntity<UserBriefDto> modify(@NotNull @PathVariable("username") String username,
                                               @NotNull @Valid @RequestBody UserUpdateDto data,
                                               Principal principal) throws NotAllowedException,
            UserNotFoundException, AuthServiceException {
        log.debug("endpoint modify a user, username={}, data={}", username, data);
        final UserDto user = userService.findByUsername(username);
        if (!user.getUsername().equals(AuthUtil.getUsername(principal))) {
            log.error("Failed to modify user: not current user {}", user.getUsername());
            throw new NotAllowedException("Failed to modify user: not current user " + user.getUsername());
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.userDtoToUserBriefDto(
                        userService.modify(user, data)));
    }

}
