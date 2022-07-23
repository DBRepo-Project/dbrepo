package at.tuwien.service;

import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.ImageNotSupportedException;
import at.tuwien.exception.QueryStoreException;

public interface QueryStoreService {

    /**
     * Creates the query store by executing a query, the Hibernate session is configured to automatically create the necessary table.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws ImageNotSupportedException The image is not supported, currently we only support MariaDB.
     * @throws QueryStoreException        The query store failed to create.
     */
    void create(Long containerId, Long databaseId) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException;
}
