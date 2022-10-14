package at.tuwien.endpoints;

import at.tuwien.api.auth.TokenBriefDto;
import at.tuwien.api.auth.TokenDto;
import at.tuwien.config.AuthenticationConfig;
import at.tuwien.entities.user.Token;
import at.tuwien.exception.TokenNotEligableException;
import at.tuwien.exception.TokenNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/user/token")
public class TokenEndpoint {

    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final AuthenticationConfig authenticationConfig;

    @Autowired
    public TokenEndpoint(UserMapper userMapper, TokenService tokenService, AuthenticationConfig authenticationConfig) {
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.authenticationConfig = authenticationConfig;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Lists developer tokens for user", security = @SecurityRequirement(name = "bearerAuth"))
    public List<TokenBriefDto> listAll(@NotNull Principal principal) throws UserNotFoundException {
        final List<Token> tokens = tokenService.findAll(principal);
        final List<TokenBriefDto> dtos = tokens.stream()
                .map(userMapper::tokenToTokenBriefDto)
                .collect(Collectors.toList());
        log.info("Found {} tokens", dtos.size());
        log.debug("found tokens {}", dtos);
        return dtos;
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create developer token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TokenDto> create(@NotNull Principal principal) throws UserNotFoundException, TokenNotEligableException {
        /* check */
        final List<Token> tokens = tokenService.findAll(principal)
                .stream()
                .filter(t -> Objects.isNull(t.getDeleted()))
                .collect(Collectors.toList());
        if (tokens.size() >= authenticationConfig.getTokenCount()) {
            log.error("Failed to create token, already exceeded maximum quota of {}", authenticationConfig.getTokenCount());
            throw new TokenNotEligableException("Failed to create token");
        }
        /* create */
        final Token token = tokenService.create(principal);
        final TokenDto dto = userMapper.tokenToTokenDto(token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @DeleteMapping("/{hash}")
    @Transactional
    @Operation(summary = "Delete developer token", security = @SecurityRequirement(name = "bearerAuth"))
    public void delete(@NotNull @PathVariable("hash") String hash,
                       @NotNull Principal principal) throws TokenNotFoundException, UserNotFoundException {
        final Token token = tokenService.findOne(hash);
        tokenService.delete(token.getTokenHash(), principal);
        log.info("Deleted token with id {}", token.getId());
        log.debug("deleted token {}", token);
    }

}