package at.ac.tuwien.ac.at.ifs.dbrepo.timer;

import at.ac.tuwien.ac.at.ifs.dbrepo.service.StorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class StaleObjectTimer {

    private final StorageService storageService;

    @Autowired
    public StaleObjectTimer(StorageService storageService) {
        this.storageService = storageService;
    }

    @Scheduled(cron = "${dbrepo.s3.cron}")
    public void deleteStaleObjects() {
        storageService.deleteStaleObjects();
    }

}
