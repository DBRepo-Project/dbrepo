package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;

import java.sql.SQLException;
import java.util.Map;

public interface QueueService {

    /**
     * Inserts data into the table of a given database.
     *
     * @param database    The database.
     * @param table    The table.
     * @param data     The data.
     * @throws SQLException The connection to the database could not be established.
     */
    void insert(DatabaseDto database, TableDto table, Map<String, Object> data) throws SQLException;

    /**
     * Inserts data and returns the created tuple including versioning timestamps.
     * Default implementation falls back to a simple insert and returns null.
     * Implementations may override to provide efficient retrieval of timestamps.
     */
    default TupleWithTimestampsDto insertWithTimestamps(DatabaseDto database, TableDto table, Map<String, Object> data) throws SQLException {
        insert(database, table, data);
        return null;
    }
}
