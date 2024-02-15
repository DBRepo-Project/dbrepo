package at.tuwien.listener.impl;

import at.tuwien.exception.*;
import at.tuwien.listener.DatabaseListener;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.StoreService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class MariadbListenerImpl implements DatabaseListener {

    private final StoreService storeService;
    private final DatabaseService databaseService;
    private final DatabaseRepository databaseRepository;

    @Autowired
    public MariadbListenerImpl(StoreService storeService, DatabaseService databaseService,
                               DatabaseRepository databaseRepository) {
        this.storeService = storeService;
        this.databaseService = databaseService;
        this.databaseRepository = databaseRepository;
        log.debug("deleting stale queries & updating metadata all 60s");
    }

    @Override
    @Scheduled(fixedRateString = "${fda.deleteStaleQueriesRate}", timeUnit = TimeUnit.SECONDS)
    @Transactional(readOnly = true)
    public void deleteStaleQueries() throws QueryStoreException, ImageNotSupportedException {
        storeService.deleteStaleQueries();
    }

    @Override
    @Scheduled(fixedRateString = "${fda.obtainMetadataRate}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void updateStoredMetadata() throws QueryMalformedException, ColumnParseException, DatabaseNotFoundException {
        for (Long databaseId : databaseRepository.findAllOnlyIds()) {
            try {
                databaseService.obtainTablesMetadata(databaseId);
                databaseService.obtainConstraints(databaseId);
                databaseService.obtainViewsMetadata(databaseId);
            } catch (DatabaseUnchangedException | TableMalformedException e) {
                /* ignore */
            }
        }
    }

}
