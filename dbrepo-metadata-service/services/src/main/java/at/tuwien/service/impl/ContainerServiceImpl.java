package at.tuwien.service.impl;

import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.ContainerAlreadyExistsException;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.mapper.ContainerMapper;
import at.tuwien.repository.mdb.ContainerRepository;
import at.tuwien.repository.mdb.ImageRepository;
import at.tuwien.service.ContainerService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class ContainerServiceImpl implements ContainerService {

    private final ContainerMapper containerMapper;
    private final ImageRepository imageRepository;
    private final ContainerRepository containerRepository;

    @Autowired
    public ContainerServiceImpl(ContainerMapper containerMapper, ImageRepository imageRepository,
                                ContainerRepository containerRepository) {
        this.containerMapper = containerMapper;
        this.imageRepository = imageRepository;
        this.containerRepository = containerRepository;
    }

    @Override
    @Transactional
    public Container create(ContainerCreateRequestDto data, Principal principal) throws ImageNotFoundException,
            ContainerAlreadyExistsException {
        /* check */
        final Optional<Container> optional = containerRepository.findByInternalName(
                containerMapper.containerToInternalContainerName(data.getName()));
        if (optional.isPresent()) {
            log.error("Failed to create container with name {}: already exists in metadata database", data.getName());
            throw new ContainerAlreadyExistsException("Failed to create container with name " + data.getName() + ": already exists in metadata database");
        }
        final Optional<ContainerImage> optional2 = imageRepository.findById(data.getImageId());
        if (optional2.isEmpty()) {
            log.error("Failed to find image with id {} in metadata database", data.getImageId());
            throw new ImageNotFoundException("Failed to find image with id " + data.getImageId() + " in metadata database");
        }
        /* entity */
        final Container container = Container.builder()
                .image(optional2.get())
                .name(data.getName())
                .internalName(containerMapper.containerToInternalContainerName(data.getName()))
                .build();
        log.info("Created container with id {} in metadata database", container.getId());
        return container;
    }

    @Override
    @Transactional
    public void remove(Long containerId) throws ContainerNotFoundException {
        /* check */
        find(containerId);
        /* delete */
        containerRepository.deleteById(containerId);
        log.info("Deleted container with id {} in metadata database", containerId);
    }

    @Override
    @Transactional
    public Container find(Long id) throws ContainerNotFoundException {
        final Optional<Container> container = containerRepository.findById(id);
        if (container.isEmpty()) {
            log.error("Failed to find container with id {} in metadata database", id);
            throw new ContainerNotFoundException("Failed to find container with id " + id + " in metadata database");
        }
        return container.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Container> getAll(Integer limit) {
        if (limit == null) {
            return containerRepository.findAll(Sort.by(Sort.Direction.DESC, "created"));
        } else {
            return containerRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "created")))
                    .toList();
        }
    }
}
