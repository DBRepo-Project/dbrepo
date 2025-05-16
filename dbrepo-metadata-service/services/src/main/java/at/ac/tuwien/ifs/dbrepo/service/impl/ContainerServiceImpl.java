package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.container.CreateContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.repository.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.repository.ImageRepository;
import at.ac.tuwien.ifs.dbrepo.service.ContainerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ContainerServiceImpl implements ContainerService {

    private final MetadataMapper metadataMapper;
    private final ImageRepository imageRepository;
    private final ContainerRepository containerRepository;

    @Autowired
    public ContainerServiceImpl(MetadataMapper metadataMapper, ImageRepository imageRepository,
                                ContainerRepository containerRepository) {
        this.metadataMapper = metadataMapper;
        this.imageRepository = imageRepository;
        this.containerRepository = containerRepository;
    }

    @Override
    @Transactional
    public Container create(CreateContainerDto data) throws ImageNotFoundException,
            ContainerAlreadyExistsException {
        final String containerName = "dbrepo-userdb-" + metadataMapper.nameToInternalName(data.getName());
        /* check */
        final Optional<Container> optional = containerRepository.findByInternalName(containerName);
        if (optional.isPresent()) {
            log.error("Failed to create container with name {}: exists in metadata database", data.getName());
            throw new ContainerAlreadyExistsException("Failed to create container: exists in metadata database");
        }
        final Optional<ContainerImage> optional2 = imageRepository.findById(data.getImageId());
        if (optional2.isEmpty()) {
            log.error("Failed to find image with id {} in metadata database", data.getImageId());
            throw new ImageNotFoundException("Failed to find image in metadata database");
        }
        /* entity */
        Container container = Container.builder()
                .image(optional2.get())
                .name(data.getName())
                .internalName(containerName)
                .host(data.getHost())
                .port(data.getPort())
                .privilegedUsername(data.getPrivilegedUsername())
                .privilegedPassword(data.getPrivilegedPassword())
                .build();
        container = containerRepository.save(container);
        log.info("Created container with id {}", container.getId());
        return container;
    }

    @Override
    @Transactional
    public void remove(Container container) throws ContainerNotFoundException {
        try {
            containerRepository.deleteById(container.getId());
            log.info("Deleted container with id {}", container.getId());
        } catch (DataAccessException e) {
            log.error("Failed to find container with id {} in metadata database", container.getId());
            throw new ContainerNotFoundException("Failed to find container in metadata database", e);
        }
    }

    @Override
    @Transactional
    public Container find(UUID id) throws ContainerNotFoundException {
        final Optional<Container> container = containerRepository.findById(id);
        if (container.isEmpty()) {
            log.error("Failed to find container with id {} in metadata database", id);
            throw new ContainerNotFoundException("Failed to find container in metadata database");
        }
        return container.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Container> getAll(Integer limit) {
        if (limit == null) {
            return containerRepository.findByOrderByCreatedDesc(Pageable.unpaged());
        } else {
            return containerRepository.findByOrderByCreatedDesc(Pageable.ofSize(limit));
        }
    }
}
