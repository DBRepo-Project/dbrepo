package at.tuwien.endpoints;

import at.tuwien.api.user.UserForgotDto;
import at.tuwien.config.SecurityConfig;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.MailService;
import at.tuwien.service.TimeSecretService;
import at.tuwien.service.UserService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.security.Principal;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/user/secret")
public class TimeSecretEndpoint {

    private final UserService userService;
    private final MailService mailService;
    private final TimeSecretService tokenService;
    private final SecurityConfig securityConfig;

    @Autowired
    public TimeSecretEndpoint(UserService userService, MailService mailService, TimeSecretService tokenService,
                              SecurityConfig securityConfig) {
        this.userService = userService;
        this.mailService = mailService;
        this.tokenService = tokenService;
        this.securityConfig = securityConfig;
    }

    @GetMapping
    @Transactional
    @Timed(value = "email.verify", description = "Time needed to verify the user email")
    @Operation(summary = "verify user email")
    public void verifyEmail(@NotNull @RequestParam String token,
                            @NotNull HttpServletResponse httpServletResponse,
                            @Null Principal principal) throws SecretInvalidException, NotAllowedException {
        log.debug("endpoint verify user email, token={}, principal={}", token, principal);
        if (principal != null) {
            log.error("Failed to verify e-mail while being logged-in");
            throw new NotAllowedException("Failed to verify e-mail while being logged-in");
        }
        tokenService.invalidate(token);
        httpServletResponse.setHeader("Location", securityConfig.getWebsite() + "/login?email_verified");
        log.debug("redirect user to website {}", securityConfig.getWebsite() + "/login?email_verified");
        httpServletResponse.setStatus(302);
    }

    @PostMapping("/resend")
    @Transactional
    @Timed(value = "email.resend", description = "Time needed to re-send the user email verification")
    @Operation(summary = "resend user token")
    public ResponseEntity<?> resend(@NotNull @Valid @RequestBody UserForgotDto data,
                                    @Null Principal principal) throws UserNotFoundException, UserEmailFailedException,
            UserEmailAlreadyVerifiedException, NotAllowedException {
        log.debug("endpoint resend user token, data={}, principal={}", data, principal);
        if (principal != null) {
            log.error("Failed to verify e-mail while being logged-in");
            throw new NotAllowedException("Failed to verify e-mail while being logged-in");
        }
        final User user = userService.findByUsernameOrEmail(data.getUsername(), data.getEmail());
        if (user.getEmailVerified()) {
            log.error("Failed to resend user token for email {}, already verified", user.getEmail());
            log.trace("failed to resend user token for user {}", user);
            throw new UserEmailAlreadyVerifiedException("Failed to resend user token, email already verified");
        }
        final TimeSecret token = tokenService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "E-Mail Verification", "mail-verify-email.txt", context);
        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

}