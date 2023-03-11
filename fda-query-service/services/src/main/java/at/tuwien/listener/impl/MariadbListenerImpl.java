package at.tuwien.listener.impl;

import at.tuwien.exception.ImageNotSupportedException;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.listener.DatabaseListener;
import at.tuwien.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MariadbListenerImpl implements DatabaseListener {

    private final StoreService storeService;

    @Autowired
    public MariadbListenerImpl(StoreService storeService) {
        this.storeService = storeService;
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *" /* at 2am, non-standard CRON syntax */)
    @Transactional(readOnly = true)
    public void deleteStaleQueries() throws QueryStoreException, ImageNotSupportedException {
        storeService.deleteStaleQueries();
    }

}
