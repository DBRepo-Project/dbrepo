package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.container.CreateContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ContainerService {

    /**
     * Creates a container.
     *
     * @param createDto The container metadata.
     * @return The container object, if successful.
     * @throws ImageNotFoundException          The image of the container was not found in the metadata database.
     * @throws ContainerAlreadyExistsException A container with this name already exists.
     */
    Container create(CreateContainerDto createDto) throws ImageNotFoundException, ContainerAlreadyExistsException;

    /**
     * Removes a container by given id from the metadata database.
     *
     * @param container The container.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     */
    void remove(Container container) throws ContainerNotFoundException;

    /**
     * Finds a container with a specific id from the metadata database.
     *
     * @param id The container id.
     * @return The container object, if successful.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     */
    Container find(UUID id) throws ContainerNotFoundException;

    /**
     * Retrieve a list of all containers from the metadata database
     *
     * @param limit Return at most this amount of results, optional.
     * @return The list of containers.
     */
    List<Container> getAll(Integer limit);
}
