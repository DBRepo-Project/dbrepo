package at.tuwien.service;

import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

public interface ContainerService {

    /**
     * Create a container with data and a given user principal.
     *
     * @param createDto The data.
     * @param principal The user principal.
     * @return The container.
     * @throws ImageNotFoundException          The container image does not exist on the server.
     * @throws DockerClientException           The container could not be created due to the Docker daemon refusing to create it.
     * @throws ContainerAlreadyExistsException A container with this name already exists.
     * @throws UserNotFoundException           The user with the user principal could not be found.
     */
    Container create(ContainerCreateRequestDto createDto, Principal principal) throws ImageNotFoundException,
            DockerClientException, ContainerAlreadyExistsException, UserNotFoundException;

    /**
     * Stops a container with id.
     *
     * @param containerId The container id.
     * @return THe container.
     * @throws ContainerNotFoundException       The container with this id could not be found in the metadata database.
     * @throws DockerClientException            The container could not be stopped due to the Docker daemon refusing to stop it.
     * @throws ContainerAlreadyStoppedException The container is already stopped.
     */
    Container stop(Long containerId) throws ContainerNotFoundException, DockerClientException, ContainerAlreadyStoppedException;

    /**
     * Removes a container with id.
     *
     * @param containerId The container id.
     * @throws ContainerNotFoundException       The container with this id could not be found in the metadata database.
     * @throws DockerClientException            The container could not be removed due to the Docker daemon refusing to removed it.
     * @throws ContainerStillRunningException   The container is still running, you need to stop it.
     * @throws ContainerAlreadyRemovedException The container is already removed.
     */
    void remove(Long containerId) throws ContainerNotFoundException, DockerClientException,
            ContainerStillRunningException, ContainerAlreadyRemovedException;

    /**
     * Finds a container with given id.
     *
     * @param containerId The container id.
     * @return The container, if successful.
     * @throws ContainerNotFoundException The container with this id could not be found in the metadata database.
     */
    Container find(Long containerId) throws ContainerNotFoundException;

    /**
     * Inspects a container with given id and retrieves metadata about it.
     *
     * @param containerId The container id.
     * @return The container, if successful.
     * @throws ContainerNotFoundException   The container with this id could not be found in the metadata database.
     * @throws DockerClientException        The container could not be inspected due to the Docker daemon refusing to inspect it.
     * @throws ContainerNotRunningException The container is not running, you need to start it.
     */
    Container inspect(Long containerId) throws ContainerNotFoundException, DockerClientException, ContainerNotRunningException;

    /**
     * Finds all containers.
     *
     * @return The list of containers.
     */
    List<Container> getAll();

    /**
     * Starts a container with given id.
     *
     * @param containerId The container id.
     * @return The container, if successful.
     * @throws ContainerNotFoundException       The container with this id could not be found in the metadata database.
     * @throws DockerClientException            The container could not be started due to the Docker daemon refusing to started it.
     * @throws ContainerAlreadyRunningException The container is already started.
     */
    Container start(Long containerId) throws ContainerNotFoundException, DockerClientException, ContainerAlreadyRunningException;
}
