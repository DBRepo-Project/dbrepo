package at.tuwien.endpoint;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.*;
import at.tuwien.config.AuthenticationConfig;
import at.tuwien.entities.user.Realm;
import at.tuwien.entities.user.Role;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.RealmService;
import at.tuwien.service.RoleService;
import at.tuwien.service.UserService;
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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/user")
public class UserEndpoint {

    private final UserMapper userMapper;
    private final RoleService roleService;
    private final UserService userService;
    private final RealmService realmService;
    private final AuthenticationConfig authenticationConfig;

    @Autowired
    public UserEndpoint(UserMapper userMapper, RoleService roleService, UserService userService,
                        RealmService realmService, AuthenticationConfig authenticationConfig) {
        this.userMapper = userMapper;
        this.roleService = roleService;
        this.userService = userService;
        this.realmService = realmService;
        this.authenticationConfig = authenticationConfig;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "user.list", description = "Time needed to list all users in the metadata database")
    @Operation(summary = "Find all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List users",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserBriefDto[].class))}),
    })
    public ResponseEntity<List<UserBriefDto>> findAll() {
        log.debug("endpoint find all users");
        final List<UserBriefDto> users = userService.findAll()
                .stream()
                .map(userMapper::userToUserBriefDto)
                .collect(Collectors.toList());
        log.trace("find all users resulted in users {}", users);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("!isAuthenticated()")
    @Timed(value = "user.create", description = "Time needed to create a user in the metadata database")
    @Operation(summary = "Create user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserBriefDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Realm or default role not found",
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
            throws RealmNotFoundException, UserAlreadyExistsException, RoleNotFoundException,
            UserEmailAlreadyExistsException {
        log.debug("endpoint create a user, data={}", data);
        /* check */
        final Realm realm = realmService.find("dbrepo");
        final Role role = roleService.find(authenticationConfig.getDefaultRole());
        userService.validateUsernameNotExists(data.getUsername());
        userService.validateEmailNotExists(data.getEmail());
        /* create */
        final User user = userService.create(data, realm, role);
        final UserBriefDto dto = userMapper.userToUserBriefDto(user);
        log.trace("create user resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/{id}")
    @Transactional
    @PreAuthorize("isAuthenticated() or hasAuthority('find-user')")
    @Timed(value = "user.info", description = "Time needed to get information of a user in the metadata database")
    @Operation(summary = "Get a user info", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find user is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> find(@NotNull @PathVariable("id") String id,
                                        @NotNull Principal principal)
            throws UserNotFoundException, NotAllowedException {
        log.debug("endpoint find a user, id={}, principal={}", id, principal);
        /* check */
        final User user = userService.find(UUID.fromString(id));
        final UserDto dto = userMapper.userToUserDto(user);
        if (user.getUsername().equals(principal.getName())) {
            log.trace("find user resulted in dto {}", dto);
            return ResponseEntity.ok()
                    .body(dto);
        } else if (User.hasRole(principal, "find-user")) {
            log.trace("find user resulted in dto {}", dto);
            return ResponseEntity.ok()
                    .body(dto);
        }
        log.error("Failed to find user: no authority and not the current logged-in user");
        throw new NotAllowedException("Failed to find user: no authority");
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('modify-user-information')")
    @Timed(value = "user.modify", description = "Time needed to modify a user in the metadata database")
    @Operation(summary = "Modify user information", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user information",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User attribute was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Modify user is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> modify(@NotNull @PathVariable("id") String id,
                                          @NotNull @Valid @RequestBody UserUpdateDto data,
                                          @NotNull Principal principal)
            throws UserNotFoundException, ForeignUserException, UserAttributeNotFoundException {
        log.debug("endpoint modify a user, id={}, data={}, principal={}", id, data, principal);
        /* check */
        final User user = userService.find(UUID.fromString(id));
        if (!user.equalsPrincipal(principal)) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        /* modify */
        final UserDto dto = userMapper.userToUserDto(userService.modify(user.getId(), data));
        log.trace("modify user resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @PutMapping("/{id}/theme")
    @Transactional
    @PreAuthorize("hasAuthority('modify-user-theme')")
    @Timed(value = "user.theme", description = "Time needed to modify a user theme in the metadata database")
    @Operation(summary = "Modify user theme", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user theme",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User or user attribute was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Modify user is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> theme(@NotNull @PathVariable("id") String id,
                                         @NotNull @Valid @RequestBody UserThemeSetDto data,
                                         @NotNull Principal principal)
            throws UserNotFoundException, ForeignUserException, UserAttributeNotFoundException {
        log.debug("endpoint modify a user theme, id={}, data={}, principal={}", id, data, principal);
        /* check */
        final User user = userService.find(UUID.fromString(id));
        if (!user.equalsPrincipal(principal)) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        /* modify theme */
        final UserDto dto = userMapper.userToUserDto(userService.toggleTheme(user.getId(), data));
        log.trace("modify user theme resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{id}/password")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    @Timed(value = "user.password", description = "Time needed to modify a user password in the metadata database")
    @Operation(summary = "Modify user password", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified user password",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Modify user is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> password(@NotNull @PathVariable("id") String id,
                                            @NotNull @Valid @RequestBody UserPasswordDto data,
                                            @NotNull Principal principal)
            throws UserNotFoundException, ForeignUserException {
        log.debug("endpoint modify a user password, id={}, data={}, principal={}", id, data, principal);
        /* check */
        final User user = userService.find(UUID.fromString(id));
        if (!user.equalsPrincipal(principal)) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        /* modify password */
        final UserDto dto = userMapper.userToUserDto(userService.updatePassword(user.getId(), data));
        log.trace("updated user password resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

}
