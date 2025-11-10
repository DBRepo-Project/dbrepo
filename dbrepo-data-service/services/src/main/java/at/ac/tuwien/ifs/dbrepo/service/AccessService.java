package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;

import java.sql.SQLException;

public interface AccessService {

    /**
     * Create a user with access to a given database.
     *
     * @param database The database.
     * @param user     The user.
     * @param access   The access type.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void create(Database database, User user, AccessTypeDto access) throws SQLException,
            DatabaseMalformedException;

    /**
     * Update access to a given database for a given user.
     *
     * @param database The database.
     * @param user     The user.
     * @param access   The access type.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void update(Database database, User user, AccessTypeDto access) throws SQLException,
            DatabaseMalformedException;

    /**
     * Revoke access to a given database for a given user.
     *
     * @param database The database.
     * @param user     The user.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void delete(Database database, User user) throws SQLException, DatabaseMalformedException;
}
