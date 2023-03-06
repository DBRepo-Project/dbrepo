package at.tuwien.endpoints;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.OrcidMalformedException;
import at.tuwien.exception.TokenRevokedException;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
    @Timed(value = "auth.create", description = "Time needed to create an authentication token")
    @Operation(summary = "Create authentication token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Authentication token created",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponseDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "418",
                    description = "User e-mail is not verified",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<JwtResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto data)
            throws UserNotFoundException, UserEmailNotVerifiedException {
        log.debug("endpoint create authentication token, data={}", data);
        final JwtResponseDto response = authenticationService.authenticate(data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping
    @Transactional
    @Timed(value = "auth.validate", description = "Time needed to validate an authentication token")
    @Operation(summary = "Validate authentication token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Authentication token is valid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))}),
            @ApiResponse(responseCode = "401",
                    description = "Authentication token is revoked",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UserDto> authenticateUser(@NotNull Principal principal,
                                                    @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization)
            throws UserNotFoundException, TokenRevokedException {
        log.debug("endpoint validate authentication token, principal={}, authorization={}", principal, authorization);
        final User user = userService.findByUsername(principal.getName());
        log.trace("authentication for principal name {} retrieved user {}", principal.getName(), user);
        final UserDto dto;
        try {
            dto = userMapper.userToUserDto(user);
        } catch (OrcidMalformedException e) {
            log.error("Failed to find user: failed to map ORCID");
            throw new UserNotFoundException("Failed to find user: failed to map ORCID");
        }
        log.trace("mapped user to dto {}", dto);
        authenticationService.verifyToken(authorization);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PostMapping("/renew")
    @Timed(value = "auth.renew", description = "Time needed to renew an authentication token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Renew authentication token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Authentication token was renewed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponseDto.class))}),
            @ApiResponse(responseCode = "401",
                    description = "Authentication token is invalid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<JwtResponseDto> reAuthenticateUser(Principal principal) {
        log.debug("endpoint renew authentication token, principal={}", principal);
        final JwtResponseDto response = authenticationService.renew(principal);
        return ResponseEntity.ok()
                .body(response);
    }

}