package at.tuwien.endpoints;

import at.tuwien.api.auth.SignupRequestDto;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    private final TimeSecretService tokenService;
    private final QueueService queueService;
    private final SecurityConfig securityConfig;

    @Autowired
    public UserEndpoint(UserMapper userMapper, UserService userService, MailService mailService,
                        TimeSecretService tokenService, QueueService queueService, SecurityConfig securityConfig) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.mailService = mailService;
        this.tokenService = tokenService;
        this.queueService = queueService;
        this.securityConfig = securityConfig;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Timed(value = "user.list", description = "Time needed to list the users")
    @Operation(summary = "List users")
    public ResponseEntity<List<UserBriefDto>> list() {
        log.debug("endpoint list users");
        final List<UserBriefDto> users = userService.findAll()
                .stream()
                .map(userMapper::userToUserBriefDto)
                .collect(Collectors.toList());
        log.info("Found {} users", users.size());
        log.trace("found users {}", users);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @Transactional
    @Timed(value = "user.create", description = "Time needed to create a user")
    @Operation(summary = "Create user")
    public ResponseEntity<UserDto> register(@NotNull @Valid @RequestBody SignupRequestDto data)
            throws UserEmailExistsException, UserNameExistsException, RoleNotFoundException, UserEmailFailedException,
            BrokerUserCreationException, OrcidMalformedException {
        log.debug("endpoint create user, data={}", data);
        final User user = userService.create(data);
        queueService.createUser(user.getUsername(), data);
        final TimeSecret token = tokenService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "Account Creation", "welcome-mail.txt", context);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("create user resulted in user {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping
    @Transactional
    @Timed(value = "user.forgot", description = "Time needed to reset a user information")
    @Operation(summary = "Forgot user information")
    public ResponseEntity<UserDto> forgot(@NotNull @Valid @RequestBody UserForgotDto data)
            throws UserNotFoundException, UserEmailFailedException, OrcidMalformedException {
        log.debug("endpoint forgot user information, data={}", data);
        final User user = userService.forgot(data);
        final TimeSecret token = tokenService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "Account Information", "forgot-mail.txt", context);
        final UserDto dto = userMapper.userToUserDto(user);
        log.trace("forgot user information resulted in user {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @PutMapping("/reset")
    @Transactional
    @Timed(value = "user.reset", description = "Time needed to reset a user information")
    @Operation(summary = "Reset user information")
    public void reset(@NotNull @Valid @RequestBody UserResetDto data,
                      @NotNull HttpServletResponse httpServletResponse) throws UserEmailFailedException,
            SecretInvalidException, UserNotFoundException, BrokerUserCreationException {
        log.debug("endpoint reset user information, data={}", data);
        final User user = tokenService.invalidate(data.getToken());
        final UserPasswordDto userPasswordDto = userMapper.userResetDtoToUserPasswordDto(data);
        userService.updatePassword(user.getId(), userPasswordDto);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        mailService.send(user, "Password Reset Successful!", "reset-mail.txt", context);
        httpServletResponse.setHeader("Location", securityConfig.getWebsite() + "/login?password_reset");
        log.debug("redirect user to website {}", securityConfig.getWebsite() + "/login?password_reset");
        httpServletResponse.setStatus(302);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Timed(value = "user.find", description = "Time needed to find a user")
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'READ_USER')")
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
        return ResponseEntity.status(HttpStatus.ACCEPTED)
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
        return ResponseEntity.status(HttpStatus.ACCEPTED)
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
            throws UserNotFoundException, BrokerUserCreationException, OrcidMalformedException {
        log.debug("endpoint update user password, id={}, data={}", id, data);
        final User entity = userService.updatePassword(id, data);
        final UserDto dto = userMapper.userToUserDto(entity);
        log.trace("update user password resulted in user {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @PutMapping("/{id}/email")
    @Transactional
    @Timed(value = "user.email", description = "Time needed to update a user email")
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_EMAIL')")
    @Operation(summary = "Update user email", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updateEmail(@NotNull @PathVariable("id") Long id,
                                               @NotNull @Valid @RequestBody UserEmailDto data)
            throws UserNotFoundException, OrcidMalformedException {
        log.debug("endpoint update user email, id={}, data={}", id, data);
        final User entity = userService.updateEmail(id, data);
        final UserDto dto = userMapper.userToUserDto(entity);
        log.trace("update user email resulted in user {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

}