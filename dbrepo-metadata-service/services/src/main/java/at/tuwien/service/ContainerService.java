package at.tuwien.service;

import at.tuwien.api.container.ContainerCreateDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;

public interface ContainerService {

    /**
     * Creates a container.
     *
     * @param createDto The container metadata.
     * @return The container object, if successful.
     * @throws ImageNotFoundException          The image of the container was not found in the metadata database.
     * @throws ContainerAlreadyExistsException A container with this name already exists.
     */
    Container create(ContainerCreateDto createDto) throws ImageNotFoundException,
            ContainerAlreadyExistsException;

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
    Container find(Long id) throws ContainerNotFoundException;

    /**
     * Retrieve a list of all containers from the metadata database
     *
     * @param limit Return at most this amount of results, optional.
     * @return The list of containers.
     */
    List<Container> getAll(Integer limit);
}
