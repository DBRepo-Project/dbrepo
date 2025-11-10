package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseGrantsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccessNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;

import java.sql.SQLException;
import java.util.Map;

public interface GrantService {

    /**
     * Finds internal access grants for a given user for a given database.
     *
     * @param database The database.
     * @param user     The user.
     * @return The internal access grants.
     * @throws AccessNotFoundException    The database access was not found.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    DatabaseGrantsDto find(Database database, User user) throws SQLException, DatabaseMalformedException,
            AccessNotFoundException;

    /**
     * Finds all database grants for a given user for a given database.
     *
     * @param database The database.
     * @param user     The user.
     * @return The database grants.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The grants could not be listed.
     */
    Map<String, DatabaseGrantsDto> findAll(Database database, User user) throws SQLException,
            DatabaseMalformedException;
}
