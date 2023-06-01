package at.tuwien.gateway;

import at.tuwien.exception.AmqpException;

public interface QueryServiceGateway {

    /**
     * Publish new data into a table with given container id, database id, table id.
     *
     * @param containerId   The container id.
     * @param databaseId    The database id.
     * @param tableId       The table id.
     * @param authorization The authentication token.
     */
    void declareConsumer(Long containerId, Long databaseId, Long tableId, String authorization) throws AmqpException;

}
