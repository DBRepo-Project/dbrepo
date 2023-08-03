package at.tuwien.config;

import at.tuwien.service.SyncService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Log4j2
public class IndexConfig {

    private final SyncService syncService;

    @Autowired
    public IndexConfig(SyncService syncService) {
        this.syncService = syncService;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        log.info("Starting to mirror the metadata database to the open search database ...");
        syncService.start();
    }
}
