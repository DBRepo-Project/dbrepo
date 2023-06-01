package at.tuwien.service;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.ContainerNotFoundException;

public interface ContainerService {
    Container find(Long id) throws ContainerNotFoundException;

    ContainerDto inspect(Long id) throws ContainerNotFoundException;
}
