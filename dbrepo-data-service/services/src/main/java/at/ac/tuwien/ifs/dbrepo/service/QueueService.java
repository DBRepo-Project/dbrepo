package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;

import java.sql.SQLException;
import java.util.Map;

public interface QueueService {

    /**
     * Inserts data into the table of a given database.
     *
     * @param database The database.
     * @param table    The table.
     * @param data     The data.
     * @throws SQLException The connection to the database could not be established.
     */
    void insert(Database database, Table table, Map<String, Object> data) throws SQLException;
}
