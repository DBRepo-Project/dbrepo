package at.tuwien.service;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.user.internal.PrivilegedUserDto;
import at.tuwien.exception.*;

import java.util.UUID;

public interface CredentialService {

    /**
     * Gets credentials for a database with given id either from the cache (if not expired) or retrieves them from the
     * Metadata Service.
     *
     * @param id The id.
     * @return The credentials.
     * @throws DatabaseNotFoundException  The database was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    PrivilegedDatabaseDto getDatabase(Long id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    /**
     * Gets credentials for a container with given id either from the cache (if not expired) or retrieves them from the
     * Metadata Service.
     *
     * @param id The container id.
     * @return The credentials.
     * @throws ContainerNotFoundException The container was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    PrivilegedContainerDto getContainer(Long id) throws ContainerNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    /**
     * Gets credentials for a table with given id in a database with given id either from the cache (if not expired) or
     * retrieves them from the Metadata Service.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @return The credentials.
     * @throws TableNotFoundException     The table was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    PrivilegedTableDto getTable(Long databaseId, Long tableId) throws RemoteUnavailableException,
            MetadataServiceException, TableNotFoundException;

    /**
     * Gets credentials for a view with given id in a database with given id either from the cache (if not expired) or
     * retrieves them from the Metadata Service.
     *
     * @param databaseId The database id.
     * @param viewId     The table id.
     * @return The credentials.
     * @throws ViewNotFoundException      The view was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    PrivilegedViewDto getView(Long databaseId, Long viewId) throws RemoteUnavailableException,
            MetadataServiceException, ViewNotFoundException;

    /**
     * Gets credentials for a container with given id either from the cache (if not expired) or retrieves them from the
     * Metadata Service.
     *
     * @param id The id.
     * @return The credentials.
     * @throws UserNotFoundException      The user was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    PrivilegedUserDto getUser(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException;

    /**
     * Gets credentials for a user with given id in a database with given id either from the cache (if not expired) or
     * retrieves them from the Metadata Service.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return The credentials.
     * @throws NotAllowedException        The access was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    DatabaseAccessDto getAccess(Long databaseId, UUID userId) throws RemoteUnavailableException,
            MetadataServiceException, NotAllowedException;

    /**
     * Invalidate the caches to force a refresh of access to a database with given id.
     *
     * @param databaseId The database id.
     */
    void invalidateAccess(Long databaseId);
}
