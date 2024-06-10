package at.tuwien.service;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.user.PrivilegedUserDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.exception.*;

import java.sql.SQLException;

public interface AccessService {

    /**
     * Create a user with access to a given database.
     * @param database The database.
     * @param user The user.
     * @param access The access type.
     * @throws SQLException The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void create(PrivilegedDatabaseDto database, PrivilegedUserDto user, AccessTypeDto access) throws SQLException,
            DatabaseMalformedException;

    /**
     * Update access to a given database for a given user.
     * @param database The database.
     * @param user The user.
     * @param access The access type.
     * @throws SQLException The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void update(PrivilegedDatabaseDto database, UserDto user, AccessTypeDto access) throws SQLException,
            DatabaseMalformedException;

    /**
     * Revoke access to a given database for a given user.
     * @param database The database.
     * @param user The user.
     * @throws SQLException The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void delete(PrivilegedDatabaseDto database, UserDto user) throws SQLException,
            DatabaseMalformedException;
}
