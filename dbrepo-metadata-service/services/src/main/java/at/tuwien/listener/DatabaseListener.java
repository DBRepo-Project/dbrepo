package at.tuwien.listener;

import at.tuwien.exception.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public interface DatabaseListener {

    /**
     * Deletes stale queries that have not been persisted within 24 hours.
     *
     * @throws QueryStoreException        The query store raised some exception.
     * @throws ImageNotSupportedException The image is not supported by the service.
     */
    @Scheduled
    void deleteStaleQueries() throws QueryStoreException, ImageNotSupportedException;

    /**
     * Updates the metadata entries in the metadata database for tables & views in the data databases.
     *
     * @throws DatabaseUnchangedException The known tables and views are up-to-date in the metadata database and no changes were made.
     * @throws QueryMalformedException    The generated SQL to obtain the metadata is malformed.
     * @throws ColumnParseException       The obtained metadata information from the views could not be parsed in known tables in the metadata database.
     * @throws DatabaseNotFoundException  The data database was not found in the metadata database.
     */
    @Scheduled
    void updateStoredMetadata() throws DatabaseUnchangedException, QueryMalformedException, ColumnParseException,
            DatabaseNotFoundException, TableNotFoundException;
}
