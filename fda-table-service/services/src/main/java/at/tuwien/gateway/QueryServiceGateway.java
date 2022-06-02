package at.tuwien.gateway;

import at.tuwien.api.database.table.TableCsvDto;

public interface QueryServiceGateway {

    /**
     * Publish new data into a table with given container id, database id, table id.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param tableId     The table id.
     * @param data        The data.
     * @return The number of inserted tuples.
     */
    Integer publish(Long containerId, Long databaseId, Long tableId, TableCsvDto data);
}
