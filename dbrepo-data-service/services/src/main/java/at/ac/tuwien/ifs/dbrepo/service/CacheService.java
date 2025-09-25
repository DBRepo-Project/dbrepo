package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.SQLException;
import java.util.UUID;

public interface CacheService {

    /**
     * Gets credentials for a database with given id either from the cache (if not expired) or retrieves them from the
     * Metadata Service.
     *
     * @param id The id.
     * @param forceReload If set to true, force a reload of the cached result. Otherwise, use the cached result if it is present.
     * @return The credentials.
     * @throws DatabaseNotFoundException  The database was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    DatabaseDto getDatabase(UUID id, boolean forceReload) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

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
    DatabaseDto getDatabase(UUID id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    TableStatisticDto getStatistic(DatabaseDto database, ViewDto view) throws TableNotFoundException,
            TableMalformedException, QueryMalformedException, SQLException;

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
    ContainerDto getContainer(UUID id) throws ContainerNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    /**
     * Gets image metadata for a image with given id either from the cache (if not expired) or retrieves the information
     * from the Metadata Service.
     *
     * @param id The image id.
     * @return The credentials.
     * @throws ImageNotFoundException The image was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    ImageDto getImage(UUID id) throws ImageNotFoundException, RemoteUnavailableException,
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
    TableDto getTable(UUID databaseId, UUID tableId) throws RemoteUnavailableException,
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
    ViewDto getView(UUID databaseId, UUID viewId) throws RemoteUnavailableException,
            MetadataServiceException, ViewNotFoundException;

    /**
     * Gets credentials for a container with given id either from the cache (if not expired) or retrieves them from the
     * Metadata Service.
     *
     * @param username The username.
     * @return The credentials.
     * @throws UserNotFoundException      The user was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    UserDto getUser(String username) throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException;

    /**
     * Gets credentials for a user with given id in a database with given id either from the cache (if not expired) or
     * retrieves them from the Metadata Service.
     *
     * @param databaseId The database id.
     * @param username     The username.
     * @return The credentials.
     * @throws NotAllowedException        The access was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available.
     * @throws MetadataServiceException   The remote service returned invalid data.
     */
    DatabaseAccessDto getAccess(UUID databaseId, String username) throws RemoteUnavailableException,
            MetadataServiceException, NotAllowedException;

    /**
     * Gets local table by remote table ID either from the cache (if not expired) or retrieves it from the Metadata Service.
     *
     * @param databaseId The local database ID context
     * @param remoteTableId The remote table ID
     * @return The local table DTO
     * @throws RemoteUnavailableException The remote service is not available
     * @throws MetadataServiceException The remote service returned invalid data
     * @throws TableNotFoundException Table not found
     */
    TableDto getLocalTableByRemoteTableId(UUID databaseId, UUID remoteTableId) throws RemoteUnavailableException,
            MetadataServiceException, TableNotFoundException;

    /**
     * Gets local database by remote database ID either from the cache (if not expired) or retrieves it from the Metadata Service.
     *
     * @param remoteDatabaseId The remote database ID
     * @return The local database DTO
     * @throws RemoteUnavailableException The remote service is not available
     * @throws MetadataServiceException The remote service returned invalid data
     * @throws DatabaseNotFoundException Database not found
     */
    DatabaseDto getLocalDatabaseByRemoteDatabaseId(UUID remoteDatabaseId) throws RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException;
}
