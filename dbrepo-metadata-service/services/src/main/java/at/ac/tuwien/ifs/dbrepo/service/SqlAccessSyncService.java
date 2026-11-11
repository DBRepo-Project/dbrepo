package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/** Applies owner-transfer grants only after their metadata transaction commits. */
@Slf4j
@Service
public class SqlAccessSyncService {
    private final DatabaseRepository databases;
    private final DataServiceGateway data;
    private final TransactionTemplate transaction;

    public SqlAccessSyncService(DatabaseRepository databases, DataServiceGateway data,
                                PlatformTransactionManager transactionManager) {
        this.databases = databases;
        this.data = data;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void afterCommit(UUID databaseId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                synchronize(databaseId);
            }
        });
    }

    @Scheduled(initialDelayString = "${dbrepo.access.retryDelayMs:30000}",
            fixedDelayString = "${dbrepo.access.retryDelayMs:30000}")
    public void retryPending() {
        databases.findPendingGrantDatabases().forEach(this::synchronize);
    }

    public void synchronize(UUID databaseId) {
        try {
            transaction.executeWithoutResult(status -> databases.findForUpdateById(databaseId).ifPresent(database -> {
                // Serialize with transfers and revocations. A retry must use current membership,
                // never the privileges from the original transfer request.
                for (var access : database.getAccesses()) {
                    if (access.isSqlGrantPending()) {
                        try {
                            data.synchronizeAccess(databaseId, access.getUsername());
                            access.setSqlGrantPending(false);
                        } catch (RuntimeException e) {
                            log.error("SQL access synchronization for database {} and user {} remains pending",
                                    databaseId, access.getUsername(), e);
                        }
                    }
                }
            }));
        } catch (RuntimeException e) {
            // The committed flag survives connection failures, process restarts and failed acknowledgements.
            log.error("SQL access synchronization for database {} remains pending", databaseId, e);
        }
    }
}
