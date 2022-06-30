package at.tuwien.endpoints;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.exception.ProcessCompletionException;
import at.tuwien.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/broker/vhost")
public class VirtualHostEndpoint {

    private final QueueService queueService;

    @Autowired
    public VirtualHostEndpoint(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Operation(summary = "Create virtual host", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> create(@NotNull @Valid @RequestBody CreateVirtualHostDto data,
                                    @NotNull Principal principal)
            throws ProcessCompletionException {
        queueService.createVirtualHost(data);
        return ResponseEntity.accepted()
                .build();
    }

}