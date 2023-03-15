package at.tuwien.endpoints;

import at.tuwien.api.auth.TokenBriefDto;
import at.tuwien.api.auth.TokenDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.config.AuthenticationConfig;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.TokenNotEligableException;
import at.tuwien.exception.TokenNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.TokenService;
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

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/user/token")
public class TokenEndpoint {

    private final UserMapper userMapper;
    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticationConfig authenticationConfig;

    @Autowired
    public TokenEndpoint(UserMapper userMapper, UserService userService, TokenService tokenService,
                         AuthenticationConfig authenticationConfig) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.tokenService = tokenService;
        this.authenticationConfig = authenticationConfig;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    @Timed(value = "token.list", description = "Time needed to list the developer tokens")
    @Operation(summary = "Lists developer tokens for user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of developer tokens",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenBriefDto[].class))}),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TokenBriefDto>> listAll(@NotNull Principal principal) throws UserNotFoundException {
        log.debug("endpoint list developer tokens, principal={}", principal);
        final List<Token> tokens = tokenService.findAll(principal);
        log.trace("found all tokens {}", tokens);
        final List<TokenBriefDto> dtos = tokens.stream()
                .map(userMapper::tokenToTokenBriefDto)
                .collect(Collectors.toList());
        log.info("Found {} tokens", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DEVELOPER')")
    @Timed(value = "token.create", description = "Time needed to create a developer token")
    @Operation(summary = "Create developer token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a developer token",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Maximum token quota exceeded",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TokenDto> create(@NotNull Principal principal) throws UserNotFoundException,
            TokenNotEligableException {
        log.debug("endpoint create developer token, principal={}", principal);
        /* check */
        final List<Token> tokens = tokenService.findAll(principal);
        log.trace("found all tokens {}", tokens);
        if (tokens.size() >= authenticationConfig.getTokenCount()) {
            log.error("Failed to create token, already exceeded maximum quota of {}", authenticationConfig.getTokenCount());
            throw new TokenNotEligableException("Failed to create token");
        }
        /* create */
        final Token token = tokenService.create(principal);
        final TokenDto dto = userMapper.tokenToTokenDto(token);
        log.trace("created developer token and resulting in {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DEVELOPER')")
    @Timed(value = "token.delete", description = "Time needed to delete the developer tokens")
    @Operation(summary = "Delete developer token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted a developer token",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Token or user not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Deletion of foreign tokens is not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long id,
                                    @NotNull Principal principal) throws TokenNotFoundException, UserNotFoundException,
            NotAllowedException {
        log.debug("endpoint delete developer token, id={}, principal={}", id, principal);
        final Token token = tokenService.findOne(id);
        final User user = userService.findByUsername(principal.getName());
        if (!token.getCreator().equals(user.getId())) {
            log.error("Failed to delete token because it is not owned by the current user");
            throw new NotAllowedException("Failed to delete token because it is not owned by the current user");
        }
        tokenService.delete(token.getTokenHash(), principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}