package at.tuwien.config;

import at.tuwien.service.SyncService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Configuration
public class SyncConfig {

    @Value("${fda.syncRate}")
    private Long syncRate;

    private final SyncService syncService;

    @Autowired
    public SyncConfig(SyncService syncService) {
        this.syncService = syncService;
        log.debug("sync rate is {} second(s)", syncRate);
    }

    @EventListener(ApplicationStartedEvent.class)
    @Transactional
    public void init() {
        syncService.start();
    }

}
