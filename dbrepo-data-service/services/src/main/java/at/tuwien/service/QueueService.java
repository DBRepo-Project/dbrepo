package at.tuwien.service;

import at.tuwien.api.database.table.TableDto;

import java.sql.SQLException;
import java.util.Map;

public interface QueueService {

    /**
     * Inserts data into the table of a given database.
     *
     * @param table    The table.
     * @param data     The data.
     * @throws SQLException The connection to the database could not be established.
     */
    void insert(TableDto table, Map<String, Object> data) throws SQLException;
}
