package at.tuwien.endpoints;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.*;
import at.tuwien.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/broker/user")
public class UserEndpoint {

    private final QueueService queueService;

    @Autowired
    public UserEndpoint(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping
    @Operation(summary = "Create user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> create(@NotNull @Valid @RequestBody CreateUserDto data) throws ProcessCompletionException {
        queueService.createUser(data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @PutMapping("/{username}/password")
    @Operation(summary = "Modifies user password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> modify(@NotNull @PathVariable("username") String username,
                                    @NotNull @Valid @RequestBody UserModifyPasswordDto data,
                                    @NotNull Principal principal)
            throws ProcessCompletionException {
        queueService.modifyPassword(username, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{username}/permission")
    @Operation(summary = "Grants user permission", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> grant(@NotNull @PathVariable("username") String username,
                                   @NotNull @Valid @RequestBody GrantVirtualHostPermissionsDto data,
                                   @NotNull Principal principal)
            throws ProcessCompletionException {
        queueService.grantVirtualHost(username, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

}