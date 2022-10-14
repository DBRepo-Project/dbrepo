package at.tuwien.endpoints;

import at.tuwien.api.user.UserForgotDto;
import at.tuwien.config.SecurityConfig;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.MailService;
import at.tuwien.service.TimeSecretService;
import at.tuwien.service.UserService;
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
    @Operation(summary = "verify user email")
    public void verifyEmail(@RequestParam String token,
                            HttpServletResponse httpServletResponse) throws SecretInvalidException {
        tokenService.invalidate(token);
        httpServletResponse.setHeader("Location", securityConfig.getWebsite() + "/login?email_verified");
        httpServletResponse.setStatus(302);
    }

    @PostMapping("/resend")
    @Transactional
    @Operation(summary = "resend user token")
    public ResponseEntity<?> resend(@NotNull @Valid @RequestBody UserForgotDto data)
            throws UserNotFoundException, UserEmailFailedException, UserEmailAlreadyVerifiedException {
        final User user = userService.findByUsernameOrEmail(data.getUsername(), data.getEmail());
        if (user.getEmailVerified()) {
            log.warn("User already has a verified email address");
            throw new UserEmailAlreadyVerifiedException("User e-mail already verified");
        }
        final TimeSecret token = tokenService.create(user);
        final Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("token", token.getToken());
        mailService.send(user, "E-Mail Verification", "token-mail.txt", context);
        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

}