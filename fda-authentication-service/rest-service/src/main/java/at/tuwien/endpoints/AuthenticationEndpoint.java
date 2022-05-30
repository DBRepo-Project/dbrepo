package at.tuwien.endpoints;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
import at.tuwien.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/auth")
public class AuthenticationEndpoint {

    private final UserMapper userMapper;
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Autowired
    public AuthenticationEndpoint(UserMapper userMapper, UserService userService,
                                  AuthenticationService authenticationService) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    @Operation(summary = "Create token")
    public ResponseEntity<JwtResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto data)
            throws UserNotFoundException, UserEmailNotVerifiedException {
        final JwtResponseDto response = authenticationService.authenticate(data);
        return ResponseEntity.accepted()
                .body(response);
    }

    @PutMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Validate token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> authenticateUser(Principal principal) throws UserNotFoundException {
        final UserDto user = userMapper.userToUserDto(userService.findByUsername(principal.getName()));
        return ResponseEntity.accepted()
                .body(user);
    }

    @PostMapping("/renew")
    @Operation(summary = "Renew token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<JwtResponseDto> reAuthenticateUser(Principal principal) {
        final JwtResponseDto response = authenticationService.renew(principal);
        return ResponseEntity.ok()
                .body(response);
    }

}