package at.tuwien.scheduler;

import at.tuwien.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    private final SyncService syncService;

    @Autowired
    public SyncScheduler(SyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(fixedRateString = "${fda.syncRate}")
    public void schedule() {
        syncService.start();
    }

}
