package at.tuwien.gateway;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.exception.ContainerNotFoundException;

public interface ContainerServiceGateway {

    /**
     * @param id
     * @return
     * @throws ContainerNotFoundException
     */
    ContainerDto find(Long id) throws ContainerNotFoundException;
}
