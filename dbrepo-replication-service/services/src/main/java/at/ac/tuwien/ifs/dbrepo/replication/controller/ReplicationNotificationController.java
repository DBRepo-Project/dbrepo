package at.ac.tuwien.ifs.dbrepo.replication.controller;

import at.ac.tuwien.ifs.dbrepo.replication.dto.TupleNotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replication")
public class ReplicationNotificationController {
    private static final Logger log = LoggerFactory.getLogger(ReplicationNotificationController.class);

    @PostMapping("/notify")
    public ResponseEntity<Void> receiveNotification(@RequestBody TupleNotificationDto notification) {
        log.info("Received tuple notification: table={}, timestamp={}, data={}",
                notification.getTableName(), notification.getTimestamp(), notification.getTupleData());
        return ResponseEntity.ok().build();
    }
} 