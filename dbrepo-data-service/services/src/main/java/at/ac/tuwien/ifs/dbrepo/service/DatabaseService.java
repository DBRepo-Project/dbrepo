package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreCreateException;

import java.sql.SQLException;

public interface DatabaseService {

    /**
     * Creates a database in the given container.
     *
     * @param container The container.
     * @param data      The database metadata.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    Database create(Container container, CreateDatabaseDto data) throws SQLException, DatabaseMalformedException;

    void createExtensions(Container container, String databaseName) throws SQLException,
            QueryStoreCreateException;

    /**
     * Creates the query store in the container and database.
     *
     * @param container    The container.
     * @param databaseName The database name.
     * @throws SQLException              The connection to the database could not be established.
     * @throws QueryStoreCreateException The query store could not be created.
     */
    void createQueryStore(Container container, String databaseName) throws SQLException,
            QueryStoreCreateException;

    /**
     * Updates a user's password in a given database.
     *
     * @param database The database.
     * @param data     The user-password tuple.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void update(Database database, UpdateUserPasswordDto data) throws SQLException, DatabaseMalformedException;
}
