package at.tuwien.scheduler;

import at.tuwien.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SyncScheduler {

    private final SyncService syncService;

    @Autowired
    public SyncScheduler(SyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(fixedRateString = "${fda.syncRate}", timeUnit = TimeUnit.SECONDS)
    public void schedule() {
        syncService.start();
    }

}
