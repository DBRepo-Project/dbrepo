package at.tuwien.endpoints;

import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.AuthenticationService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
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

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/user")
public class UserEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final MetadataMapper userMapper;
    private final DatabaseService databaseService;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserEndpoint(UserService userService, MetadataMapper userMapper, DatabaseService databaseService,
                        AuthenticationService authenticationService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.databaseService = databaseService;
        this.authenticationService = authenticationService;
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
    })
    public ResponseEntity<List<UserBriefDto>> findAll(@RequestParam(required = false) String username) {
        log.debug("endpoint find all users, username={}", username);
        if (username == null) {
            return ResponseEntity.ok(userService.findAll()
                    .stream()
                    .filter(user -> !user.getIsInternal())
                    .map(userMapper::userToUserBriefDto)
                    .toList());
        }
        log.trace("filter by username: {}", username);
        try {
            final User user = userService.findByUsername(username);
            if (user.getIsInternal()) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(List.of(userMapper.userToUserBriefDto(user)));
        } catch (UserNotFoundException e) {
            log.trace("filter by username {} failed: return empty list", username);
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_user_create")
    @Operation(summary = "Create user",
            description = "This webhook is called from the auth service to add a user to the metadata database. Requires role `system`.",
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Parameters are not well-formed (likely email)",
                    content = {@Content(mediaType = "application/json")}),
    })
    public ResponseEntity<UserBriefDto> create(@NotNull @Valid @RequestBody CreateUserDto data) {
        log.debug("endpoint create user, data.id={}, data.username={}", data.getId(), data.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.userToUserBriefDto(
                        userService.create(data)));
    }

    @RequestMapping(value = "/{userId}", method = {RequestMethod.GET, RequestMethod.HEAD})
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> find(@NotNull @PathVariable("userId") UUID userId,
                                        @NotNull Principal principal) throws NotAllowedException,
            UserNotFoundException {
        log.debug("endpoint find a user, userId={}, principal.name={}", userId, principal.getName());
        /* check */
        final User user = userService.findById(userId);
        if (!user.getId().equals(getId(principal)) && !hasRole(principal, "find-foreign-user")) {
            log.error("Failed to find user: foreign user");
            throw new NotAllowedException("Failed to find user: foreign user");
        }
        if (user.getIsInternal()) {
            log.error("Failed to find user: internal user");
            throw new NotAllowedException("Failed to find user: internal user");
        }
        final HttpHeaders headers = new HttpHeaders();
        if (isSystem(principal)) {
            headers.set("X-Username", user.getUsername());
            headers.set("X-Password", user.getMariadbPassword());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(userMapper.userToUserDto(user));
    }

    @PutMapping("/{userId}")
    @Transactional
    @PreAuthorize("hasAuthority('modify-user-information')")
    @Observed(name = "dbrepo_user_modify")
    @Operation(summary = "Update user",
            description = "Updates user with id. Requires role `modify-user-information`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user information",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Modify user query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to modify user metadata",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to modify user at auth service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserBriefDto> modify(@NotNull @PathVariable("userId") UUID userId,
                                               @NotNull @Valid @RequestBody UserUpdateDto data,
                                               @NotNull Principal principal) throws NotAllowedException,
            UserNotFoundException, AuthServiceException {
        log.debug("endpoint modify a user, userId={}, data={}", userId, data);
        final User user = userService.findById(userId);
        if (!user.getId().equals(getId(principal))) {
            log.error("Failed to modify user: not current user {}", user.getId());
            throw new NotAllowedException("Failed to modify user: not current user " + user.getId());
        }
        return ResponseEntity.accepted()
                .body(userMapper.userToUserBriefDto(
                        userService.modify(user, data)));
    }

}
