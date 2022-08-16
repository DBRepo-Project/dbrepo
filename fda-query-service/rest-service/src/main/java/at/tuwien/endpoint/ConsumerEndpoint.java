package at.tuwien.endpoint;

import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/consumer")
public class ConsumerEndpoint extends AbstractEndpoint {

    private final TableService tableService;
    private final MessageQueueService messageQueueService;

    @Autowired
    public ConsumerEndpoint(DatabaseService databaseService, IdentifierService identifierService,
                            TableService tableService, MessageQueueService messageQueueService) {
        super(databaseService, identifierService);
        this.tableService = tableService;
        this.messageQueueService = messageQueueService;
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Declare consumer", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> declare(@NotNull @PathVariable("id") Long containerId,
                                        @NotNull @PathVariable("databaseId") Long databaseId,
                                        @NotNull @PathVariable("tableId") Long tableId,
                                        @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, AmqpException, NotAllowedException {
        if (!hasDatabasePermission(containerId, databaseId, "QUEUE_CREATE_CONSUMER", principal)) {
            log.error("Missing data export permission");
            throw new NotAllowedException("Missing data export permission");
        }
        final Table table = tableService.find(containerId, databaseId, tableId);
        messageQueueService.createConsumer(table.getTopic(), containerId, databaseId, tableId);
        return ResponseEntity.accepted()
                .build();
    }

}
