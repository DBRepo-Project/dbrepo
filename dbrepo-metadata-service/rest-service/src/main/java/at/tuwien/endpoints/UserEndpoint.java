package at.tuwien.endpoints;

import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.auth.RefreshTokenRequestDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.AuthenticationService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import at.tuwien.utils.UserUtil;
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
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/user")
public class UserEndpoint {

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
            description = "Lists users known to the metadata database.")
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
                    .map(userMapper::userToUserBriefDto)
                    .toList());
        }
        try {
            log.trace("filter by username: {}", username);
            return ResponseEntity.ok(List.of(userMapper.userToUserBriefDto(userService.findByUsername(username))));
        } catch (UserNotFoundException e) {
            log.trace("filter by username {} failed: return empty list", username);
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("!isAuthenticated()")
    @Observed(name = "dbrepo_user_create")
    @Operation(summary = "Create user",
            description = "Creates a user in the auth service and metadata database. Requires that no credentials are sent in the request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Parameters are not well-formed (likely email)",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "403",
                    description = "Internal authentication to the auth service is invalid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Default role not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "User with username already exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "User with e-mail already exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Failed to create in auth service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to create in auth service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> create(@NotNull @Valid @RequestBody SignupRequestDto data)
            throws UserExistsException, EmailExistsException, AuthServiceException, AuthServiceConnectionException,
            UserNotFoundException, CredentialsInvalidException {
        log.debug("endpoint create user, data.username={}", data.getUsername());
        userService.validateUsernameNotExists(data.getUsername());
        userService.validateEmailNotExists(data.getEmail());
        final User user = userService.create(data, authenticationService.create(data).getAttributes().getLdapId()[0]);
        log.info("Created user with id: {}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.userToUserDto(user));
    }

    @PostMapping("/token")
    @Observed(name = "dbrepo_user_token")
    @Operation(summary = "Create token",
            description = "Creates a user token via the auth service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Obtained user token",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid login request",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to get token",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find user in auth database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "428",
                    description = "Account is not fully setup in auth service (requires password change?)",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to auth service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to get user in auth service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TokenDto> getToken(@NotNull @Valid @RequestBody LoginRequestDto data)
            throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException, CredentialsInvalidException,
            AccountNotSetupException {
        log.debug("endpoint get token, data.username={}", data.getUsername());
        /* check */
        final TokenDto token = authenticationService.obtainToken(data);
        try {
            userService.findByUsername(data.getUsername());
        } catch (UserNotFoundException e) {
            /* need to sync */
            log.warn("User with username {} does not exist in metadata database yet", data.getUsername());
            final SignupRequestDto request = SignupRequestDto.builder()
                    .username(data.getUsername())
                    .email("noreply@example.com")
                    .password(data.getPassword())
                    .build();
            final at.tuwien.api.keycloak.UserDto user = authenticationService.findByUsername(data.getUsername());
            if (user.getAttributes().getLdapId().length != 1) {
                log.error("Failed to map ldap id for user with username: {}", data.getUsername());
                throw new UserNotFoundException("Failed to map ldap id");
            }
            userService.create(request, user.getAttributes().getLdapId()[0]);
            log.info("Patched missing user information for user with username: {}", data.getUsername());
        }
        return ResponseEntity.accepted()
                .body(token);
    }

    @PutMapping("/token")
    @Observed(name = "dbrepo_user_refresh_token")
    @Operation(summary = "Refresh token",
            description = "Refreshes user token by refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Refreshed user token",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid refresh token",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to auth service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TokenDto> refreshToken(@NotNull @Valid @RequestBody RefreshTokenRequestDto data)
            throws AuthServiceConnectionException, CredentialsInvalidException {
        log.debug("endpoint refresh token");
        /* check */
        final TokenDto token = authenticationService.refreshToken(data.getRefreshToken());
        return ResponseEntity.accepted()
                .body(token);
    }

    @GetMapping("/{userId}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    @Observed(name = "dbrepo_user_find")
    @Operation(summary = "Get user",
            description = "Gets user with id from the metadata database. Requires authentication.",
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
        if (!user.equals(principal)) {
            if (!UserUtil.hasRole(principal, "find-foreign-user")) {
                log.error("Failed to find user: foreign user");
                throw new NotAllowedException("Failed to find user: foreign user");
            }
        }
        final UserDto dto = userMapper.userToUserDto(user);
        return ResponseEntity.ok()
                .body(dto);
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
    })
    public ResponseEntity<UserDto> modify(@NotNull @PathVariable("userId") UUID userId,
                                          @NotNull @Valid @RequestBody UserUpdateDto data,
                                          @NotNull Principal principal) throws NotAllowedException,
            UserNotFoundException, DatabaseNotFoundException {
        log.debug("endpoint modify a user, userId={}, data={}", userId, data);
        User user = userService.findById(userId);
        if (!user.equals(principal)) {
            log.error("Failed to modify user: not current user");
            throw new NotAllowedException("Failed to modify user: not current user");
        }
        user = userService.modify(user, data);
        final UserDto dto = userMapper.userToUserDto(user);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{userId}/password")
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("isAuthenticated()")
    @Observed(name = "dbrepo_user_password_modify")
    @Operation(summary = "Update user password",
            description = "Updates password of user with id. Requires authentication.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user password"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid password payload",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to change foreign user password",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to auth service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to get user in auth service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> password(@NotNull @PathVariable("userId") UUID userId,
                                         @NotNull @Valid @RequestBody UserPasswordDto data,
                                         @NotNull Principal principal) throws NotAllowedException, AuthServiceException,
            AuthServiceConnectionException, UserNotFoundException, DatabaseNotFoundException, DataServiceException,
            DataServiceConnectionException, CredentialsInvalidException {
        log.debug("endpoint modify a user password, userId={}, data.password=(hidden)", userId);
        final User user = userService.findById(userId);
        if (!user.equals(principal)) {
            log.error("Failed to modify user password: not current user");
            throw new NotAllowedException("Failed to modify user password: not current user");
        }
        authenticationService.updatePassword(user, data);
        for (Database database : databaseService.findAllAccess(userId)) {
            databaseService.updatePassword(database, user);
        }
        userService.updatePassword(user, data);
        return ResponseEntity.accepted()
                .build();
    }

}
