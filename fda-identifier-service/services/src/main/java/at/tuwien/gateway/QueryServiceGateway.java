package at.tuwien.gateway;

import at.tuwien.api.database.query.ExportDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

@Service
public interface QueryServiceGateway {

    /**
     * Finds a query by given id from the query service that internally looks in the query store of a container.
     *
     * @param containerId   The container id.
     * @param databaseId    The database id.
     * @param identifier    The identifier containing the query id and database id.
     * @param authorization The authorization token.
     * @return The query information if successful.
     * @throws QueryNotFoundException     The query was not found.
     * @throws RemoteUnavailableException The remote service is not available.
     */
    QueryDto find(Long containerId, Long databaseId, IdentifierCreateDto identifier, String authorization)
            throws QueryNotFoundException, RemoteUnavailableException;

    /**
     * Exports a query by given id.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param queryId     The query id.
     * @return The exported resource as bytes.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws QueryNotFoundException     The query was not found.
     */
    byte[] export(Long containerId, Long databaseId, Long queryId) throws RemoteUnavailableException,
            QueryNotFoundException;
}
