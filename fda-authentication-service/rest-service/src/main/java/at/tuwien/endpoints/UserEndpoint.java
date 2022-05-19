package at.tuwien.endpoints;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.RoleNotFoundException;
import at.tuwien.exception.UserEmailExistsException;
import at.tuwien.exception.UserEmailFailedException;
import at.tuwien.exception.UserNameExistsException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.MailService;
import at.tuwien.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

import javax.validation.Valid;
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

    @Autowired
    public UserEndpoint(UserMapper userMapper, UserService userService, MailService mailService) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.mailService = mailService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD') or hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "List users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<UserDto>> list() {
        final List<User> users = userService.findAll();
        return ResponseEntity.ok(users.stream()
                .map(userMapper::userToUserDto)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @Operation(summary = "Create user")
    public ResponseEntity<UserDto> register(@Valid @RequestBody SignupRequestDto data) throws UserEmailExistsException,
            UserNameExistsException, RoleNotFoundException, UserEmailFailedException {
        final User user = userService.create(data);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        mailService.send(user, "Account Creation", "welcome-mail.txt", context);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.userToUserDto(user));
    }

}