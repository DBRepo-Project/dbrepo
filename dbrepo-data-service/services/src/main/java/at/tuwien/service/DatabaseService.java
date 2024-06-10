package at.tuwien.service;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.exception.*;

import java.sql.SQLException;

public interface DatabaseService {

    /**
     * Creates a database in the given container.
     * @param container The container.
     * @param data The database metadata.
     * @return The created database, if successful.
     * @throws SQLException The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    PrivilegedDatabaseDto create(PrivilegedContainerDto container, CreateDatabaseDto data) throws SQLException,
            DatabaseMalformedException;

    /**
     * Updates a user's password in a given database.
     * @param database The database.
     * @param data The user-password tuple.
     * @throws SQLException The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void update(PrivilegedDatabaseDto database, UpdateUserPasswordDto data) throws SQLException,
            DatabaseMalformedException;
}
