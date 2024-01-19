package at.tuwien.endpoints;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.UserService;
import at.tuwien.utils.PrincipalUtil;
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
@RequestMapping("/api/user")
public class UserEndpoint {

    private final UserMapper userMapper;
    private final UserService userService;
    private final DatabaseService databaseService;
    private final MessageQueueService messageQueueService;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserEndpoint(UserMapper userMapper, UserService userService, DatabaseService databaseService,
                        MessageQueueService messageQueueService, AuthenticationService authenticationService) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.databaseService = databaseService;
        this.messageQueueService = messageQueueService;
        this.authenticationService = authenticationService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_users_findall")
    @Operation(summary = "Find all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List users",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserBriefDto.class)))}),
    })
    public ResponseEntity<List<UserBriefDto>> findAll() {
        log.debug("endpoint find all users");
        final List<UserBriefDto> users = userService.findAll()
                .stream()
                .map(userMapper::userToUserBriefDto)
                .toList();
        log.trace("find all users resulted in users {}", users);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("!isAuthenticated()")
    @Observed(name = "dbr_user_create")
    @Operation(summary = "Create user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Parameters are not well-formed (likely email)",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "404",
                    description = "default role not found",
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
    })
    public ResponseEntity<UserBriefDto> create(@NotNull @Valid @RequestBody SignupRequestDto data)
            throws UserAlreadyExistsException, UserEmailAlreadyExistsException, UserNotFoundException,
            KeycloakRemoteException, AccessDeniedException, BrokerRemoteException,
            BrokerVirtualHostModificationException {
        log.debug("endpoint create a user, data={}", data);
        /* check */
        userService.validateUsernameNotExists(data.getUsername());
        userService.validateEmailNotExists(data.getEmail());
        /* create */
        authenticationService.create(data);
        final at.tuwien.api.keycloak.UserDto keycloakUserDto = authenticationService.findByUsername(data.getUsername());
        try {
            messageQueueService.createUser(data.getUsername(), data.getPassword());
            messageQueueService.setVirtualHostPermissions(data.getUsername());
        } catch (BrokerRemoteException | BrokerVirtualHostGrantException e) {
            try {
                authenticationService.delete(keycloakUserDto.getId());
            } catch (UserNotFoundException e2) {
                /* ignore */
            }
            throw new BrokerRemoteException(e);
        } catch (BrokerVirtualHostModificationException e) {
            try {
                authenticationService.delete(keycloakUserDto.getId());
            } catch (UserNotFoundException e2) {
                /* ignore */
            }
            throw new BrokerVirtualHostModificationException(e);
        }
        final User user = userService.create(data, keycloakUserDto.getId());
        final UserBriefDto dto = userMapper.userToUserBriefDto(user);
        log.trace("create user resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/{id}")
    @Transactional
    @PreAuthorize("isAuthenticated() or hasAuthority('find-user')")
    @Observed(name = "dbr_user_find")
    @Operation(summary = "Get a user info", security = @SecurityRequirement(name = "bearerAuth"))
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
    public ResponseEntity<UserDto> find(@NotNull @PathVariable("id") UUID id,
                                        @NotNull Principal principal) throws UserNotFoundException,
            NotAllowedException {
        log.debug("endpoint find a user, id={}, {}", id, PrincipalUtil.formatForDebug(principal));
        /* check */
        final User user = userService.find(id);
        final UserDto dto = userMapper.userToUserDto(user);
        if (user.getUsername().equals(principal.getName())) {
            log.trace("find user resulted in dto {}", dto);
            return ResponseEntity.ok()
                    .body(dto);
        } else if (UserUtil.hasRole(principal, "find-user")) {
            log.trace("find user resulted in dto {}", dto);
            return ResponseEntity.ok()
                    .body(dto);
        }
        log.error("Failed to find user: no authority and not the current logged-in user");
        throw new NotAllowedException("Failed to find user: no authority and not the current logged-in user");
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('modify-user-information')")
    @Observed(name = "dbr_user_modify")
    @Operation(summary = "Modify user information", security = @SecurityRequirement(name = "bearerAuth"))
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
                    description = "Modify user is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User attribute was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Foreign user modification",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> modify(@NotNull @PathVariable("id") UUID id,
                                          @NotNull @Valid @RequestBody UserUpdateDto data,
                                          @NotNull Principal principal) throws UserNotFoundException,
            ForeignUserException, QueryMalformedException {
        log.debug("endpoint modify a user, id={}, data={}, {}", id, data, PrincipalUtil.formatForDebug(principal));
        /* check */
        if (!id.equals(UserUtil.getId(principal))) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        /* modify */
        final User user = userService.modify(id, data);
        databaseService.updatePassword(user);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("modify user resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @PutMapping("/{id}/theme")
    @Transactional
    @PreAuthorize("hasAuthority('modify-user-theme')")
    @Observed(name = "dbr_user_theme_modify")
    @Operation(summary = "Modify user theme", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user theme",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Modify user is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User or user attribute was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Foreign user modification",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> theme(@NotNull @PathVariable("id") UUID id,
                                         @NotNull @Valid @RequestBody UserThemeSetDto data,
                                         @NotNull Principal principal) throws UserNotFoundException,
            ForeignUserException {
        log.debug("endpoint modify a user theme, id={}, data={}, {}", id, data, PrincipalUtil.formatForDebug(principal));
        /* check */
        if (!id.equals(UserUtil.getId(principal))) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        /* modify theme */
        final User user = userService.toggleTheme(id, data);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("modify user theme resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{id}/password")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    @Observed(name = "dbr_user_password_modify")
    @Operation(summary = "Modify user password", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user password",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Modify is not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Foreign user modification",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Authentication service does not respond",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> password(@NotNull @PathVariable("id") UUID id,
                                      @NotNull @Valid @RequestBody UserPasswordDto data,
                                      @NotNull Principal principal)
            throws UserNotFoundException, ForeignUserException, KeycloakRemoteException, AccessDeniedException {
        log.debug("endpoint modify a user password, id={}, data={}, {}", id, data, PrincipalUtil.formatForDebug(principal));
        /* check */
        if (!id.equals(UserUtil.getId(principal))) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        /* modify password */
        userService.updatePassword(id, data);
        authenticationService.updatePassword(id, data);
        return ResponseEntity.accepted()
                .build();
    }

}
