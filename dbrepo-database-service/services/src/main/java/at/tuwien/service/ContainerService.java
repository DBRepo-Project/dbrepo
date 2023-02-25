package at.tuwien.service;

import at.tuwien.entities.container.Container;
import at.tuwien.exception.ContainerNotFoundException;

public interface ContainerService {

    /**
     * Finds a container with given id.
     *
     * @param id The container id.
     * @return The container, if successful.
     * @throws ContainerNotFoundException The container with this id was not found in the metadata database.
     */
    Container find(Long id) throws ContainerNotFoundException;
}
