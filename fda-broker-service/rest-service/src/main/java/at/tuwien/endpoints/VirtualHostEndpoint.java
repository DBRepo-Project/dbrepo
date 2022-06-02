package at.tuwien.endpoints;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.exception.ProcessCompletionException;
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
@RequestMapping("/api/broker/vhost")
public class VirtualHostEndpoint {

    private final QueueService queueService;

    @Autowired
    public VirtualHostEndpoint(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping
    @Operation(summary = "Create virtual host")
    public ResponseEntity<?> create(@NotNull @Valid @RequestBody CreateVirtualHostDto data)
            throws ProcessCompletionException {
        queueService.createVirtualHost(data);
        return ResponseEntity.accepted()
                .build();
    }

}