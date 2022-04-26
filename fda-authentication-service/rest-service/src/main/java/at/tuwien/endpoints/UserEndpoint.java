package at.tuwien.endpoints;

import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
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

    @Autowired
    public UserEndpoint(UserMapper userMapper, UserService userService) {
        this.userMapper = userMapper;
        this.userService = userService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD') or hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "List users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<UserDto>> list() {
        final List<User> users = userService.findAll();
        return ResponseEntity.ok(users.stream()
                .map(userMapper::userToUserDto)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create user")
    public ResponseEntity<UserDto> register(@Valid @RequestBody SignupRequestDto data) throws UserEmailExistsException,
            UserNameExistsException, RoleNotFoundException {
        final User user = userService.create(data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.userToUserDto(user));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'READ_USER')")
    @Operation(summary = "Find some user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> find(@NotNull @PathVariable("id") Long id) throws UserNotFoundException {
        final User entity = userService.find(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_USER')")
    @Operation(summary = "Update user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> update(@NotNull @PathVariable("id") Long id,
                                          @Valid @RequestBody UserUpdateDto data) throws UserNotFoundException {
        final User entity = userService.update(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}/roles")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Update user roles", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updateRoles(@NotNull @PathVariable("id") Long id,
                                               @Valid @RequestBody UserRolesDto data)
            throws UserNotFoundException, RoleNotFoundException, RoleUniqueException {
        final User entity = userService.updateRoles(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}/password")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_PASSWORD')")
    @Operation(summary = "Update user password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updatePassword(@NotNull @PathVariable("id") Long id,
                                                  @Valid @RequestBody UserPasswordDto data)
            throws UserNotFoundException {
        final User entity = userService.updatePassword(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

    @PutMapping("/{id}/email")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasPermission(#id, 'UPDATE_EMAIL')")
    @Operation(summary = "Update user email", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> updateEmail(@NotNull @PathVariable("id") Long id,
                                               @Valid @RequestBody UserEmailDto data) throws UserNotFoundException {
        final User entity = userService.updateEmail(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userMapper.userToUserDto(entity));
    }

}