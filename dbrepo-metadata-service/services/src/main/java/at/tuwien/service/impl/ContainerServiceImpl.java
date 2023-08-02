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
    public ContainerServiceImpl(ContainerRepository containerRepository, ImageRepository imageRepository,
                                ContainerMapper containerMapper) {
        this.imageRepository = imageRepository;
        this.containerRepository = containerRepository;
        this.containerMapper = containerMapper;
    }

    @Override
    @Transactional
    public Container create(ContainerCreateRequestDto createDto, Principal principal) throws ImageNotFoundException,
            ContainerAlreadyExistsException {
        final Optional<ContainerImage> image = imageRepository.findById(createDto.getImageId());
        if (image.isEmpty()) {
            log.error("failed to get image with id {}", createDto.getImageId());
            throw new ImageNotFoundException("image was not found in metadata database.");
        }
        /* entity */
        Container container = new Container();
        container.setImageId(image.get().getId());
        container.setName(createDto.getName());
        container.setInternalName(containerMapper.containerToInternalContainerName(container));
        /* check duplicate */
        final Optional<Container> optional = containerRepository.findByInternalName(container.getInternalName());
        if (optional.isPresent()) {
            log.error("Failed to create container with internal name {}, it already exists", container.getInternalName());
            throw new ContainerAlreadyExistsException("Container name already exists");
        }
        log.info("Created container {}", container.getId());
        return container;
    }

    @Override
    @Transactional
    public void remove(Long containerId) throws ContainerNotFoundException {
        final Container container = find(containerId);
        containerRepository.deleteById(containerId);
        log.info("Removed container with id {}", containerId);
    }

    @Override
    @Transactional
    public Container find(Long id) throws ContainerNotFoundException {
        final Optional<Container> container = containerRepository.findById(id);
        if (container.isEmpty()) {
            log.error("failed to get container with id {}", id);
            throw new ContainerNotFoundException("no container with this id in metadata database");
        }
        return container.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Container> getAll(Integer limit) {
        final List<Container> containers;
        if (limit == null) {
            containers = containerRepository.findAll(Sort.by(Sort.Direction.DESC, "created"));
        } else {
            containers = containerRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "created")))
                    .toList();
        }
        log.info("Found {} containers", containers.size());
        return containers;
    }
}
