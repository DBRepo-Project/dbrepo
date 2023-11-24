package at.tuwien.service;

import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;

import java.sql.SQLException;
import java.util.Map;

public interface QueueService {
    /**
     * Inserts data into the table of a given database.
     *
     * @param database The database name.
     * @param table    The table name.
     * @param data     The data.
     */
    void insert(String database, String table, Map<String, Object> data) throws DatabaseNotFoundException, QueryMalformedException, TableNotFoundException, SQLException;
}
