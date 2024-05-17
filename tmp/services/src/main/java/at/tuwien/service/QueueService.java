package at.tuwien.service;

import at.tuwien.api.database.table.internal.PrivilegedTableDto;

import java.sql.SQLException;
import java.util.Map;

public interface QueueService {

    /**
     * Inserts data into the table of a given database.
     *
     * @param table    The table.
     * @param data     The data.
     */
    void insert(PrivilegedTableDto table, Map<String, Object> data) throws SQLException;
}
