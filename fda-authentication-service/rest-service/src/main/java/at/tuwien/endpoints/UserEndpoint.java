package at.tuwien.endpoints;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.config.SecurityConfig;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.RoleNotFoundException;
import at.tuwien.exception.UserEmailExistsException;
import at.tuwien.exception.UserEmailFailedException;
import at.tuwien.exception.UserNameExistsException;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.MailService;
import at.tuwien.service.QueueService;
import at.tuwien.service.TokenService;
import at.tuwien.service.UserService;
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
import java.security.Principal;
import java.util.LinkedList;
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
    private final TokenService tokenService;
    private final QueueService queueService;
    private final SecurityConfig securityConfig;

    @Autowired
    public UserEndpoint(UserMapper userMapper, UserService userService, MailService mailService,
                        TokenService tokenService, QueueService queueService, SecurityConfig securityConfig) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.mailService = mailService;
        this.tokenService = tokenService;
        this.queueService = queueService;
        this.securityConfig = securityConfig;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD') or hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "List users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<UserDto>> list() throws OrcidMalformedException {
        final List<User> users = userService.findAll();
        final List<UserDto> out = new LinkedList<>();
        for (User user : users) {
            out.add(userMapper.userToUserDto(user));
        }
        return ResponseEntity.ok(out);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create user")
    public ResponseEntity<UserDto> register(@NotNull @Valid @RequestBody SignupRequestDto data)
            throws UserEmailExistsException,
            UserNameExistsException, RoleNotFoundException, UserEmailFailedException, BrokerUserCreationException, OrcidMalformedException {
        final User user = userService.create(data);
        queueService.createUser(data);
        final Token token = tokenService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "Account Creation", "welcome-mail.txt", context);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.userToUserDto(user));
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Forgot user information")
    public ResponseEntity<UserDto> forgot(@NotNull @Valid @RequestBody UserForgotDto data)
            throws UserNotFoundException, UserEmailFailedException, OrcidMalformedException {
        final User user = userService.forgot(data);
        final Token token = tokenService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "Account Information", "forgot-mail.txt", context);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(user));
    }

    @PutMapping("/reset")
    @Transactional
    @Operation(summary = "Reset user information")
    public void reset(@NotNull @Valid @RequestBody UserResetDto data,
                      @NotNull HttpServletResponse httpServletResponse)
            throws UserEmailFailedException, TokenInvalidException, UserNotFoundException, BrokerUserCreationException {
        final User user = tokenService.invalidate(data.getToken());
        final UserPasswordDto userPasswordDto = userMapper.userResetDtoToUserPasswordDto(data);
        userService.updatePassword(user.getId(), userPasswordDto);
        queueService.modifyUserPassword(user, userPasswordDto);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        mailService.send(user, "Password Reset Successful!", "reset-mail.txt", context);
        httpServletResponse.setHeader("Location", securityConfig.getWebsite() + "/login?password_reset");
        httpServletResponse.setStatus(302);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'READ_USER')")
    @Operation(summary = "Find some user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> find(@NotNull @PathVariable("id") Long id) throws UserNotFoundException, OrcidMalformedException {
        final User entity = userService.find(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_USER')")
    @Operation(summary = "Update user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> update(@NotNull @PathVariable("id") Long id,
                                          @NotNull @Valid @RequestBody UserUpdateDto data)
            throws UserNotFoundException, OrcidMalformedException {
        final User entity = userService.update(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}/roles")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Update user roles", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updateRoles(@NotNull @PathVariable("id") Long id,
                                               @NotNull @Valid @RequestBody UserRolesDto data)
            throws UserNotFoundException, RoleNotFoundException, RoleUniqueException, OrcidMalformedException {
        final User entity = userService.updateRoles(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}/theme")
    @Transactional
    @PreAuthorize("hasPermission(#id, 'UPDATE_THEME')")
    @Operation(summary = "Update user theme", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> updateTheme(@NotNull @PathVariable("id") Long id,
                                               @NotNull @Valid @RequestBody UserThemeSetDto data) throws UserNotFoundException {
        userService.updateTheme(id, data);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{id}/password")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_PASSWORD')")
    @Operation(summary = "Update user password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updatePassword(@NotNull @PathVariable("id") Long id,
                                                  @NotNull @Valid @RequestBody UserPasswordDto data)
            throws UserNotFoundException, BrokerUserCreationException, OrcidMalformedException {
        final User entity = userService.updatePassword(id, data);
        queueService.modifyUserPassword(entity, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}/email")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_EMAIL')")
    @Operation(summary = "Update user email", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updateEmail(@NotNull @PathVariable("id") Long id,
                                               @NotNull @Valid @RequestBody UserEmailDto data)
            throws UserNotFoundException, OrcidMalformedException {
        final User entity = userService.updateEmail(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

}