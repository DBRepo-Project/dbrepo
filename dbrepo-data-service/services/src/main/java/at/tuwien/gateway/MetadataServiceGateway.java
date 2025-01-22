package at.tuwien.gateway;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.exception.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public interface MetadataServiceGateway {

    /**
     * Get a container with given id from the metadata service.
     *
     * @param containerId The container id
     * @return The container with  connection information, if successful.
     * @throws ContainerNotFoundException The table was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    ContainerDto getContainerById(Long containerId) throws RemoteUnavailableException,
            ContainerNotFoundException, MetadataServiceException;

    /**
     * Get a database with given id from the metadata service.
     *
     * @param id The database id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException  The database was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    DatabaseDto getDatabaseById(Long id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    /**
     * Get a table with given database id and table id from the metadata service.
     *
     * @param databaseId The database id.
     * @param id         The table id.
     * @return The table, if successful.
     * @throws TableNotFoundException     The table was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    TableDto getTableById(Long databaseId, Long id) throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    /**
     * Get a view with given database id and view id from the metadata service.
     *
     * @param databaseId The database id.
     * @param id         The view id.
     * @return The view, if successful.
     * @throws ViewNotFoundException      The view was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    ViewDto getViewById(Long databaseId, Long id) throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException;

    /**
     * Get a user with given user id from the metadata service.
     *
     * @param userId The user id.
     * @return The user, if successful.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws UserNotFoundException      The user was not found in the metadata service.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    UserDto getUserById(UUID userId) throws RemoteUnavailableException, UserNotFoundException, MetadataServiceException;

    /**
     * Get database access for a given user and database id from the metadata service.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return The database access, if successful.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws NotAllowedException        The access to this database is denied for the given user.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    DatabaseAccessDto getAccess(Long databaseId, UUID userId) throws RemoteUnavailableException, NotAllowedException,
            MetadataServiceException;

    /**
     * Get a list of identifiers for a given database id and optional subset id.
     *
     * @param databaseId The database id.
     * @param subsetId   The subset id. Optional.
     * @return The list of identifiers.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws DatabaseNotFoundException  The database was not found.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    List<IdentifierBriefDto> getIdentifiers(@NotNull Long databaseId, Long subsetId) throws MetadataServiceException,
            RemoteUnavailableException, DatabaseNotFoundException;

    /**
     * Update the table statistics in the metadata service.
     *
     * @param databaseId    The database id.
     * @param tableId       The table id.
     * @param authorization The authorization header.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws TableNotFoundException     The table was not found.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    void updateTableStatistics(Long databaseId, Long tableId, String authorization) throws TableNotFoundException,
            MetadataServiceException, RemoteUnavailableException;
}
