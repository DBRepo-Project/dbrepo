package at.tuwien.service;

import at.tuwien.entities.container.Container;
import at.tuwien.exception.ContainerNotFoundException;

public interface ContainerService {

    /**
     * Finds a container with a specific id from the metadata database.
     *
     * @param id The container id.
     * @return The container object, if successful.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     */
    Container find(Long id) throws ContainerNotFoundException;

}
