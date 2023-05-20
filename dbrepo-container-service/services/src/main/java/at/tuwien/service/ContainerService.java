package at.tuwien.service;

import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

public interface ContainerService {

    /**
     * Creates a container.
     *
     * @param createDto The container metadata.
     * @param principal The principal of the creating user.
     * @return The container object, if successful.
     * @throws ImageNotFoundException          The image of the container was not found in the metadata database.
     * @throws DockerClientException           The docker client was unable to perform this action.
     * @throws ContainerAlreadyExistsException A container with this name already exists.
     * @throws UserNotFoundException           The user creating the container was not found in the metadata database.
     */
    Container create(ContainerCreateRequestDto createDto, Principal principal) throws ImageNotFoundException,
            DockerClientException, ContainerAlreadyExistsException, UserNotFoundException;

    /**
     * Stops a container by given id from the metadata database.
     *
     * @param containerId The container id.
     * @return The container object, if successful.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     * @throws DockerClientException      The docker client was unable to perform this action.
     */
    Container stop(Long containerId) throws ContainerNotFoundException, DockerClientException, ContainerAlreadyStoppedException;

    /**
     * Removes a stopped container by given id from the metadata database.
     *
     * @param containerId The container id.
     * @throws ContainerNotFoundException     The container was not found in the metadata database.
     * @throws DockerClientException          The docker client was unable to perform this action.
     * @throws ContainerStillRunningException The container is still running and this action cannot be performed.
     */
    void remove(Long containerId) throws ContainerNotFoundException, DockerClientException,
            ContainerStillRunningException, ContainerAlreadyRemovedException;

    /**
     * Finds a container with a specific id from the metadata database.
     *
     * @param id The container id.
     * @return The container object, if successful.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     */
    Container find(Long id) throws ContainerNotFoundException;

    /**
     * Inspects a container state and resources by given id.
     *
     * @param id The container id.
     * @return The container object.
     * @throws DockerClientException        The docker client was unable to perform this action.
     * @throws ContainerNotRunningException The docker container is not running.
     */
    ContainerDto inspect(Long id) throws DockerClientException, ContainerNotRunningException, ContainerNotFoundException;

    /**
     * Retrieve a list of all containers from the metadata database
     *
     * @param limit Return at most this amount of results, optional.
     * @return The list of containers.
     */
    List<Container> getAll(Integer limit);

    /**
     * Find all containers on the server.
     *
     * @return List of containers.
     */
    List<com.github.dockerjava.api.model.Container> list();

    /**
     * Starts a container with given id from the metadata database.
     *
     * @param containerId The container id.
     * @return The container object, if successful.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     * @throws DockerClientException      The docker client was unable to perform this action.
     */
    Container start(Long containerId) throws ContainerNotFoundException, DockerClientException, ContainerAlreadyRunningException;
}
