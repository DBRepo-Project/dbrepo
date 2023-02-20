package at.tuwien.endpoints;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.auth.TokenDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.*;
import at.tuwien.config.SecurityConfig;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.User;
import at.tuwien.exception.RoleNotFoundException;
import at.tuwien.exception.UserEmailExistsException;
import at.tuwien.exception.UserEmailFailedException;
import at.tuwien.exception.UserNameExistsException;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.MailService;
import at.tuwien.service.QueueService;
import at.tuwien.service.TimeSecretService;
import at.tuwien.service.UserService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import joptsimple.internal.Strings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/user")
public class UserEndpoint {

    private final UserMapper userMapper;
    private final UserService userService;
    private final MailService mailService;
    private final TimeSecretService timeSecretService;
    private final QueueService queueService;
    private final SecurityConfig securityConfig;

    @Autowired
    public UserEndpoint(UserMapper userMapper, UserService userService, MailService mailService,
                        TimeSecretService timeSecretService, QueueService queueService, SecurityConfig securityConfig) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.mailService = mailService;
        this.timeSecretService = timeSecretService;
        this.queueService = queueService;
        this.securityConfig = securityConfig;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "user.list", description = "Time needed to list the users")
    @Operation(summary = "List users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of users",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserBriefDto[].class))}),
    })
    public ResponseEntity<List<UserBriefDto>> list() {
        log.debug("endpoint list users");
        final List<UserBriefDto> users = userService.findAll()
                .stream()
                .map(userMapper::userToUserBriefDto)
                .collect(Collectors.toList());
        log.info("Found {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @Transactional
    @Timed(value = "user.create", description = "Time needed to create a user")
    @Operation(summary = "Create user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Form contains invalid ORCID or misses fields",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User role not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Creation of user is not allowed while logged-in",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "User failed to create at broker service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Username exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "E-Mail address exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "428",
                    description = "Sending e-mail failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> register(@NotNull @Valid @RequestBody SignupRequestDto data,
                                            @Null Principal principal)
            throws UserEmailExistsException, UserNameExistsException, RoleNotFoundException, UserEmailFailedException,
            BrokerUserCreationException, OrcidMalformedException, NotAllowedException {
        log.debug("endpoint create user, data={}, principal={}", data, principal);
        if (principal != null) {
            log.error("Failed to create user while being logged-in");
            throw new NotAllowedException("Failed to create user while being logged-in");
        }
        final User user = userService.create(data);
        queueService.createUser(user.getUsername(), data);
        final TimeSecret token = timeSecretService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "Account Creation", "mail-welcome.txt", context);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("create user resulted in user {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping
    @Transactional
    @Timed(value = "user.forgot", description = "Time needed to request a new user password")
    @Operation(summary = "Request a new user password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Successfully requested a new user password",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Form contains invalid ORCID or misses fields",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Reset of password is not allowed while logged-in",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "428",
                    description = "Sending e-mail failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> forgot(@NotNull @Valid @RequestBody UserForgotDto data,
                                          @Null Principal principal)
            throws UserNotFoundException, UserEmailFailedException, OrcidMalformedException, NotAllowedException {
        log.debug("endpoint request a new user password, data={}, principal={}", data, principal);
        if (principal != null) {
            log.error("Failed to request a new user password while being logged-in");
            throw new NotAllowedException("Failed to request a new user password while being logged-in");
        }
        final User user = userService.forgot(data);
        final TimeSecret token = timeSecretService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "Account Information", "mail-request-password-reset.txt", context);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("forgot user information resulted in user {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/reset")
    @Transactional
    @Timed(value = "user.reset", description = "Time needed to reset a user password")
    @Operation(summary = "Reset user password")
    public void reset(@NotNull @Valid @RequestBody UserResetDto data,
                      @NotNull HttpServletResponse httpServletResponse,
                      @Null Principal principal) throws UserEmailFailedException,
            SecretInvalidException, UserNotFoundException, BrokerUserCreationException, NotAllowedException {
        log.debug("endpoint reset user information, data={}, principal={}", data, principal);
        if (principal != null) {
            log.error("Failed to reset user password while being logged-in");
            throw new NotAllowedException("Failed to reset user password while being logged-in");
        }
        final User user = timeSecretService.invalidate(data.getToken());
        final UserPasswordDto userPasswordDto = userMapper.userResetDtoToUserPasswordDto(data);
        userService.updatePassword(user.getId(), userPasswordDto);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        mailService.send(user, "Password Reset Successful!", "mail-password-changed.txt", context);
        httpServletResponse.setHeader("Location", securityConfig.getWebsite() + "/login?password_reset");
        log.debug("redirect user to website {}", securityConfig.getWebsite() + "/login?password_reset");
        httpServletResponse.setStatus(302);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Timed(value = "user.find", description = "Time needed to find a user")
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Find some user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> find(@NotNull @PathVariable("id") Long id) throws UserNotFoundException,
            OrcidMalformedException {
        log.debug("endpoint find user, id={}", id);
        final User entity = userService.find(id);
        final UserDto dto = userMapper.userToUserDto(entity);
        log.trace("find user resulted in user {}", dto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(dto);
    }

    @PutMapping("/{id}")
    @Transactional
    @Timed(value = "user.update", description = "Time needed to update a user")
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_USER')")
    @Operation(summary = "Update user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> update(@NotNull @PathVariable("id") Long id,
                                          @NotNull @Valid @RequestBody UserUpdateDto data)
            throws UserNotFoundException, OrcidMalformedException {
        log.debug("endpoint update user, id={}, data={}", id, data);
        final User entity = userService.update(id, data);
        final UserDto dto = userMapper.userToUserDto(entity);
        log.trace("update user resulted in user {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{id}/roles")
    @Transactional
    @Timed(value = "user.roles", description = "Time needed to update a user role")
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Update user roles", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updateRoles(@NotNull @PathVariable("id") Long id,
                                               @NotNull @Valid @RequestBody UserRolesDto data)
            throws UserNotFoundException, RoleNotFoundException, RoleUniqueException, OrcidMalformedException {
        log.debug("endpoint update user roles, id={}, data={}", id, data);
        final User entity = userService.updateRoles(id, data);
        final UserDto dto = userMapper.userToUserDto(entity);
        log.trace("update user roles resulted in user {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{id}/theme")
    @Transactional
    @Timed(value = "user.theme", description = "Time needed to update a user theme")
    @PreAuthorize("hasPermission(#id, 'UPDATE_THEME')")
    @Operation(summary = "Update user theme", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> updateTheme(@NotNull @PathVariable("id") Long id,
                                            @NotNull @Valid @RequestBody UserThemeSetDto data)
            throws UserNotFoundException {
        log.debug("endpoint update user theme, id={}, data={}", id, data);
        userService.updateTheme(id, data);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{id}/password")
    @Transactional
    @Timed(value = "user.password", description = "Time needed to update a user password")
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_PASSWORD')")
    @Operation(summary = "Update user password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updatePassword(@NotNull @PathVariable("id") Long id,
                                                  @NotNull @Valid @RequestBody UserPasswordDto data)
            throws UserNotFoundException, BrokerUserCreationException, OrcidMalformedException,
            UserEmailFailedException {
        log.debug("endpoint update user password, id={}, data={}", id, data);
        final User user = userService.updatePassword(id, data);
        /* modify broker service */
        final UserDetailsDto details = queueService.findUser(user.getUsername());
        final CreateUserDto modifyDto = userMapper.userPasswordDtoToCreateUserDto(data);
        if (details.getTags().length > 0) {
            final String tags = Strings.join(details.getTags(), ",");
            log.debug("found tags, setting the tags={}", tags);
            modifyDto.setTags(tags);
        }
        queueService.modifyUserPassword(user, modifyDto);
        log.info("Updated broker service password for user with id {}", user.getId());
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        mailService.send(user, "Password Reset Successful!", "mail-password-changed.txt", context);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("update user password resulted in user {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{id}/email")
    @Transactional
    @Timed(value = "user.email", description = "Time needed to update a user email")
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_EMAIL')")
    @Operation(summary = "Update user email", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updateEmail(@NotNull @PathVariable("id") Long id,
                                               @NotNull @Valid @RequestBody UserEmailDto data)
            throws UserNotFoundException, OrcidMalformedException, UserEmailFailedException {
        log.debug("endpoint update user email, id={}, data={}", id, data);
        final User user = userService.updateEmail(id, data);
        final TimeSecret token = timeSecretService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "E-Mail Verification", "mail-verify-email.txt", context);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("update user email resulted in user {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

}