package at.ac.tuwien.ifs.dbrepo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TupleReplicationOutboxScheduler {

    private final TupleReplicationNotificationDispatcher dispatcher;

    @Value("${dbrepo.replication.tupleOutbox.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${dbrepo.replication.tupleOutbox.retryDelayMs:30000}")
    public void retryDueEntries() {
        if (!enabled) {
            return;
        }
        final int sent = dispatcher.dispatchDue();
        if (sent > 0) {
            log.info("Sent {} pending tuple replication notifications", sent);
        }
    }
}
