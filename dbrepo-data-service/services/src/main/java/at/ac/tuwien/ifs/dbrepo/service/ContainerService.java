package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreCreateException;

import java.sql.SQLException;

public interface ContainerService {

    /**
     * Creates a database in the given container.
     * @param container The container.
     * @param data The database metadata.
     * @return The created database, if successful.
     * @throws SQLException The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    DatabaseDto createDatabase(ContainerDto container, CreateDatabaseDto data) throws SQLException,
            DatabaseMalformedException;

    /**
     * Creates the query store in the container and database.
     *
     * @param container    The container.
     * @param databaseName The database name.
     * @throws SQLException              The connection to the database could not be established.
     * @throws QueryStoreCreateException The query store could not be created.
     */
    void createQueryStore(ContainerDto container, String databaseName) throws SQLException, QueryStoreCreateException;
}
