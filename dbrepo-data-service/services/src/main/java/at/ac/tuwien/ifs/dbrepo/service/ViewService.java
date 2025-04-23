package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewMalformedException;

import java.sql.SQLException;
import java.time.Instant;

public interface ViewService {

    /**
     * Deletes a view.
     *
     * @param database The database.
     * @param view The view.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    void delete(DatabaseDto database, ViewDto view) throws SQLException, ViewMalformedException;

    /**
     * Counts tuples in a view at system-versioned timestamp.
     *
     * @param database The database.
     * @param view      The view.
     * @param timestamp The system-versioned timestamp.
     * @return The number of tuples.
     * @throws SQLException            The connection to the data database was unsuccessful.
     * @throws QueryMalformedException The query is malformed and was rejected by the data database.
     */
    Long count(DatabaseDto database, ViewDto view, Instant timestamp) throws SQLException, QueryMalformedException;
}
