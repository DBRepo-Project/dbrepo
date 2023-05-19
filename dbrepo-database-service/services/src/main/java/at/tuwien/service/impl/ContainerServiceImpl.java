package at.tuwien.service.impl;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.gateway.ContainerServiceGateway;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.service.ContainerService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
public class ContainerServiceImpl implements ContainerService {

    private final ContainerRepository containerRepository;
    private final ContainerServiceGateway containerServiceGateway;

    @Autowired
    public ContainerServiceImpl(ContainerRepository containerRepository,
                                ContainerServiceGateway containerServiceGateway) {
        this.containerRepository = containerRepository;
        this.containerServiceGateway = containerServiceGateway;
    }

    @Override
    public Container find(Long id) throws ContainerNotFoundException {
        final Optional<Container> optional = containerRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find container with id {}", id);
            throw new ContainerNotFoundException("Failed to find container");
        }
        return optional.get();
    }

    @Override
    public ContainerDto inspect(Long id) throws ContainerNotFoundException {
        return containerServiceGateway.find(id);
    }
}
