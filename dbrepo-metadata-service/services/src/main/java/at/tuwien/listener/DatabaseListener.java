package at.tuwien.listener;

import at.tuwien.exception.ImageNotSupportedException;
import at.tuwien.exception.QueryStoreException;
import org.springframework.scheduling.annotation.Scheduled;

public interface DatabaseListener {

    /**
     * Deletes stale queries that have not been persisted within 24 hours.
     *
     * @throws QueryStoreException        The query store raised some exception.
     * @throws ImageNotSupportedException The image is not supported by the service.
     */
    @Scheduled
    void deleteStaleQueries() throws QueryStoreException, ImageNotSupportedException;
}
