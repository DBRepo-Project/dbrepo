package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
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
    ContainerDto getContainerById(UUID containerId) throws RemoteUnavailableException,
            ContainerNotFoundException, MetadataServiceException;

    /**
     * Get a image with given id from the metadata service.
     *
     * @param imageId The image id
     * @return The image with data type and operator information, if successful.
     * @throws ImageNotFoundException The image was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    ImageDto getImageById(UUID imageId) throws RemoteUnavailableException,
            ImageNotFoundException, MetadataServiceException;

    /**
     * Get a database with given id from the metadata service.
     *
     * @param id The database id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException  The database was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    DatabaseDto getDatabaseById(UUID id) throws DatabaseNotFoundException, RemoteUnavailableException,
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
    TableDto getTableById(UUID databaseId, UUID id) throws TableNotFoundException, RemoteUnavailableException,
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
    ViewDto getViewById(UUID databaseId, UUID id) throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException;

    /**
     * Get a user with given user id from the metadata service.
     *
     * @param username The user username.
     * @return The user, if successful.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws UserNotFoundException      The user was not found in the metadata service.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    UserDto getUserByUsername(String username) throws RemoteUnavailableException, UserNotFoundException, MetadataServiceException;

    /**
     * Get database access for a given user and database id from the metadata service.
     *
     * @param databaseId The database id.
     * @param username     The username.
     * @return The database access, if successful.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws NotAllowedException        The access to this database is denied for the given user.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    DatabaseAccessDto getAccess(UUID databaseId, String username) throws RemoteUnavailableException, NotAllowedException,
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
    List<IdentifierBriefDto> getIdentifiers(@NotNull UUID databaseId, UUID subsetId) throws MetadataServiceException,
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
    void updateTableStatistics(UUID databaseId, UUID tableId, String authorization) throws TableNotFoundException,
            MetadataServiceException, RemoteUnavailableException;

    /**
     * Get local table by remote table ID from the metadata service.
     *
     * @param databaseId The local database ID context
     * @param remoteTableId The remote table ID
     * @return The local table DTO
     * @throws RemoteUnavailableException The remote service is not available
     * @throws MetadataServiceException The remote service returned invalid data
     * @throws TableNotFoundException Table not found
     */
    TableDto getLocalTableByRemoteTableId(UUID databaseId, UUID remoteTableId)
            throws RemoteUnavailableException, MetadataServiceException, TableNotFoundException;

    /**
     * Get local database by remote database ID from the metadata service.
     *
     * @param remoteDatabaseId The remote database ID
     * @return The local database DTO
     * @throws RemoteUnavailableException The remote service is not available
     * @throws MetadataServiceException The remote service returned invalid data
     * @throws DatabaseNotFoundException Database not found
     */
    DatabaseDto getLocalDatabaseByRemoteDatabaseId(UUID remoteDatabaseId)
            throws RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException;
}
