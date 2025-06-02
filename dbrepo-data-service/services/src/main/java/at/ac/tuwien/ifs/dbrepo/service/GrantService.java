package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseGrantsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;

import java.sql.SQLException;
import java.util.Map;

public interface GrantService {

    /**
     * Finds database access grants for a given user for a given database.
     * @param database The database.
     * @param user The user.
     * @return The database access grants.
     * @throws SQLException               The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    DatabaseGrantsDto find(DatabaseDto database, UserDto user) throws SQLException, DatabaseMalformedException;

    Map<String, DatabaseGrantsDto> findAll(DatabaseDto database, UserDto user) throws SQLException, DatabaseMalformedException;
}
