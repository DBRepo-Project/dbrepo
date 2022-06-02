package at.tuwien.endpoints;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.exception.*;
import at.tuwien.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
    @Operation(summary = "Create user")
    public ResponseEntity<?> create(@NotNull @Valid @RequestBody CreateUserDto data) throws ProcessCompletionException {
        queueService.createUser(data);
        return ResponseEntity.accepted()
                .build();
    }

}