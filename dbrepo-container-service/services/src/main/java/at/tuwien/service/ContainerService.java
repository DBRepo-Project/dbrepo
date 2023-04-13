package at.tuwien.service;

import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;

public interface ContainerService {

    /**
     * @param createDto
     * @param principal
     * @return
     * @throws ImageNotFoundException
     * @throws DockerClientException
     * @throws ContainerAlreadyExistsException
     * @throws UserNotFoundException
     */
    Container create(ContainerCreateRequestDto createDto, Principal principal) throws ImageNotFoundException,
            DockerClientException, ContainerAlreadyExistsException, UserNotFoundException;

    /**
     * @param containerId
     * @return
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     */
    Container stop(Long containerId) throws ContainerNotFoundException, DockerClientException, ContainerAlreadyStoppedException;

    /**
     * @param containerId
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     * @throws ContainerStillRunningException
     */
    void remove(Long containerId) throws ContainerNotFoundException, DockerClientException,
            ContainerStillRunningException, ContainerAlreadyRemovedException;

    /**
     * @param id
     * @return
     * @throws ContainerNotFoundException
     */
    Container find(Long id) throws ContainerNotFoundException;

    /**
     * @param id
     * @return
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     * @throws ContainerNotRunningException
     */
    Container inspect(Long id) throws ContainerNotFoundException, DockerClientException, ContainerNotRunningException;

    /**
     * Retrieve a list of all containers from the metadata database
     *
     * @param limit Return at most this amount of results, optional.
     * @return The list of containers.
     */
    List<Container> getAll(Integer limit);

    List<com.github.dockerjava.api.model.Container> list();

    /**
     * @param containerId
     * @return
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     */
    Container start(Long containerId) throws ContainerNotFoundException, DockerClientException, ContainerAlreadyRunningException;
}
