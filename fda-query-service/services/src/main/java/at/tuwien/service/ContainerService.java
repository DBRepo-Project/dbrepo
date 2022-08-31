package at.tuwien.service;

import at.tuwien.entities.container.Container;
import at.tuwien.exception.ContainerNotFoundException;

public interface ContainerService {

    /**
     * Finds a container with given id.
     *
     * @param id The container id.
     * @return The container.
     * @throws ContainerNotFoundException The container was not found.
     */
    Container find(Long id) throws ContainerNotFoundException;
}
