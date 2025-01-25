package at.tuwien.service;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.QueryStoreCreateException;

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
