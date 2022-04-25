package at.tuwien.endpoints;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final UserDetailsService userDetailsServiceImpl;
    private final AuthenticationService authenticationService;

    @Autowired
    public AuthenticationEndpoint(UserMapper userMapper, UserDetailsService userDetailsServiceImpl,
                                  AuthenticationService authenticationService) {
        this.userMapper = userMapper;
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    @Operation(summary = "Create token")
    public ResponseEntity<JwtResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto data) {
        final JwtResponseDto response = authenticationService.authenticate(data);
        return ResponseEntity.accepted()
                .body(response);
    }

    @PutMapping
    @Operation(summary = "Validate token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> authenticateUser(Principal principal) {
        final UserDetails details = userDetailsServiceImpl.loadUserByUsername(principal.getName());
        return ResponseEntity.accepted()
                .body(userMapper.userDetailsToUserDto(details, principal));
    }

    @PostMapping("/renew")
    @Operation(summary = "Renew token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<JwtResponseDto> reAuthenticateUser(Principal principal) {
        final JwtResponseDto response = authenticationService.renew(principal);
        return ResponseEntity.ok()
                .body(response);
    }

}