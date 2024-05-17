package at.tuwien.gateway;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.PrivilegedUserDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.exception.*;

import java.util.List;
import java.util.UUID;

public interface MetadataServiceGateway {

    /**
     * Get a container with given id from the metadata service.
     *
     * @param containerId The container id
     * @return The container with privileged connection information, if successful.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws ContainerNotFoundException The container was not found in the metadata service.
     */
    PrivilegedContainerDto getContainerById(Long containerId) throws RemoteUnavailableException, ContainerNotFoundException;

    /**
     * Get all databases from the metadata service.
     *
     * @return List of databases, if successful.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     */
    List<PrivilegedDatabaseDto> getDatabases() throws RemoteUnavailableException;

    void updateTableStatistics(Long databaseId, Long tableId, TableStatisticDto data)
            throws RemoteUnavailableException;

    /**
     * Get a database with given id from the metadata service.
     *
     * @param id The database id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException  The database was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     */
    PrivilegedDatabaseDto getDatabaseById(Long id) throws DatabaseNotFoundException, RemoteUnavailableException;

    /**
     * Get a database with given internal name from the metadata service.
     *
     * @param internalName The internal name.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException  The database was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     */
    PrivilegedDatabaseDto getDatabaseByInternalName(String internalName) throws DatabaseNotFoundException, RemoteUnavailableException;

    /**
     * Get a table with given database id and table id from the metadata service.
     *
     * @param databaseId The database id.
     * @param id         The table id.
     * @return The table, if successful.
     * @throws TableNotFoundException     The table was not found in the metadata service.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     */
    PrivilegedTableDto getTableById(Long databaseId, Long id) throws TableNotFoundException, RemoteUnavailableException;

    PrivilegedViewDto getViewById(Long databaseId, Long id) throws RemoteUnavailableException, ViewNotFoundException;

    /**
     * Get a user with given user id from the metadata service.
     *
     * @param userId The user id.
     * @return The user, if successful.
     * @throws RemoteUnavailableException The remote service is not available and invalid data was returned.
     * @throws UserNotFoundException      The user was not found in the metadata service.
     */
    PrivilegedUserDto getUserById(UUID userId) throws RemoteUnavailableException, UserNotFoundException;

    DatabaseAccessDto getAccess(Long databaseId, UUID userId) throws RemoteUnavailableException, NotAllowedException;

    List<IdentifierDto> getIdentifiers(Long databaseId, Long subsetId) throws RemoteUnavailableException,
            NotAllowedException;

    List<IdentifierDto> getIdentifiers(Long databaseId) throws RemoteUnavailableException,
            NotAllowedException;

    UserDto getUser(UUID userId) throws RemoteUnavailableException, NotAllowedException, UserNotFoundException;
}
