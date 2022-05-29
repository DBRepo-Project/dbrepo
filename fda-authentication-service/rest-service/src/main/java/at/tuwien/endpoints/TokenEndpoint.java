package at.tuwien.endpoints;

import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.MailService;
import at.tuwien.service.TokenService;
import at.tuwien.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/user/token")
public class TokenEndpoint {

    private final UserService userService;
    private final MailService mailService;
    private final TokenService tokenService;

    @Autowired
    public TokenEndpoint(UserService userService, MailService mailService, TokenService tokenService) {
        this.userService = userService;
        this.mailService = mailService;
        this.tokenService = tokenService;
    }

    @GetMapping
    @Transactional
    @Operation(summary = "verify user email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) throws TokenInvalidException {
        tokenService.invalidate(token);
        return ResponseEntity.accepted()
                .body("Verification successful.");
    }

    @GetMapping("/resend")
    @Transactional
    @Operation(summary = "resend user token")
    public ResponseEntity<?> resend(@RequestParam(required = false) String username,
                                    @RequestParam(required = false) String email)
            throws UserNotFoundException, UserEmailFailedException, UserEmailAlreadyVerifiedException {
        final User user = userService.findByUsernameOrEmail(username, email);
        if (user.getEmailVerified()) {
            log.warn("User already has a verified email address");
            throw new UserEmailAlreadyVerifiedException("User e-mail already verified");
        }
        final Token token = tokenService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "E-Mail Verification", "token-mail.txt", context);
        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

}