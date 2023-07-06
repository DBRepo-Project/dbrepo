package at.tuwien.gateway;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.exception.ContainerNotFoundException;

public interface ContainerServiceGateway {

    /**
     * Finds a container by given id.
     *
     * @param id The container id.
     * @return The container.
     * @throws ContainerNotFoundException The container was not found.
     */
    ContainerDto find(Long id) throws ContainerNotFoundException;
}
