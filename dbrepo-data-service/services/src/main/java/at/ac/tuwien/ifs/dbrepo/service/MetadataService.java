package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.util.UUID;

public interface MetadataService {

    /**
     * Finds connection information and metadata for a database with given id from the metadata service. This result
     * needs to be cached to increase performance of the data service.
     *
     * @param id The database id.
     * @return The database.
     * @throws DatabaseNotFoundException  The database was not found in the metadata service.
     * @throws RemoteUnavailableException The metadata service responded unexpectedly or not at all.
     * @throws MetadataServiceException   The metadata service could not process the request.
     */
    Database getDatabase(UUID id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    /**
     * Finds metadata for a user with given username from the metadata service. This result needs to be cached to
     * increase performance of the data service.
     *
     * @param username The username.
     * @return The user.
     * @throws UserNotFoundException      The user was not found in the metadata service.
     * @throws RemoteUnavailableException The metadata service responded unexpectedly or not at all.
     * @throws MetadataServiceException   The metadata service could not process the request.
     */
    User getUser(String username) throws RemoteUnavailableException, MetadataServiceException, UserNotFoundException;

    /**
     * Finds metadata for an image with given id from the metadata service. This result needs to be cached to
     * increase performance of the data service.
     *
     * @param id The image id.
     * @return The image.
     * @throws ImageNotFoundException     The image was not found in the metadata service.
     * @throws RemoteUnavailableException The metadata service responded unexpectedly or not at all.
     * @throws MetadataServiceException   The metadata service could not process the request.
     */
    Image getImage(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ImageNotFoundException;

    /**
     * Finds the connection information and metadata for a container with given id from the metadata service. This
     * result needs to be cached to increase performance of the data service.
     *
     * @param id The container id.
     * @return The container.
     * @throws ContainerNotFoundException The container was not found in the metadata service.
     * @throws RemoteUnavailableException The metadata service responded unexpectedly or not at all.
     * @throws MetadataServiceException   The metadata service could not process the request.
     */
    Container getContainer(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException;

    /**
     * Finds the connection information and metadata for a table with given database id and id from the metadata
     * service. This result needs to be cached to increase performance of the data service.
     *
     * @param databaseId The database id.
     * @param id         The table id.
     * @return The table.
     * @throws TableNotFoundException     The table was not found in the metadata service.
     * @throws RemoteUnavailableException The metadata service responded unexpectedly or not at all.
     * @throws MetadataServiceException   The metadata service could not process the request.
     */
    Table getTable(UUID databaseId, UUID id) throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException;

    /**
     * Finds the connection information and metadata for a view with given database id and id from the metadata
     * service. This result needs to be cached to increase performance of the data service.
     *
     * @param databaseId The database id.
     * @param id         The view id.
     * @return The view.
     * @throws ViewNotFoundException      The view was not found in the metadata service.
     * @throws RemoteUnavailableException The metadata service responded unexpectedly or not at all.
     * @throws MetadataServiceException   The metadata service could not process the request.
     */
    View getView(UUID databaseId, UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ViewNotFoundException;
}
