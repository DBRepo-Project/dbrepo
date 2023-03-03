package at.tuwien.service.impl;

import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.ContainerMapper;
import at.tuwien.mapper.ImageMapper;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.UserService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.SocketUtils;

import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class ContainerServiceImpl implements ContainerService {

    private final HostConfig hostConfig;
    private final ImageMapper imageMapper;
    private final UserService userService;
    private final DockerClient dockerClient;
    private final DockerConfig dockerConfig;
    private final ContainerMapper containerMapper;
    private final ImageRepository imageRepository;
    private final ContainerRepository containerRepository;

    @Autowired
    public ContainerServiceImpl(DockerClient dockerClient, ContainerRepository containerRepository,
                                ImageRepository imageRepository, HostConfig hostConfig, ContainerMapper containerMapper,
                                ImageMapper imageMapper, UserService userService, DockerConfig dockerConfig) {
        this.hostConfig = hostConfig;
        this.dockerClient = dockerClient;
        this.imageRepository = imageRepository;
        this.containerRepository = containerRepository;
        this.containerMapper = containerMapper;
        this.imageMapper = imageMapper;
        this.userService = userService;
        this.dockerConfig = dockerConfig;
    }

    @Override
    @Transactional
    public Container create(ContainerCreateRequestDto createDto, Principal principal) throws ImageNotFoundException,
            DockerClientException, ContainerAlreadyExistsException, UserNotFoundException {
        final Optional<ContainerImage> image = imageRepository.findByRepositoryAndTag(createDto.getRepository(),
                createDto.getTag());
        if (image.isEmpty()) {
            log.error("failed to get image with name {}:{}", createDto.getRepository(), createDto.getTag());
            throw new ImageNotFoundException("image was not found in metadata database.");
        }
        /* entity */
        final Integer availableTcpPort = SocketUtils.findAvailableTcpPort(10000);
        Container container = new Container();
        container.setImageId(image.get().getId());
        container.setPort(availableTcpPort);
        container.setName(createDto.getName());
        container.setInternalName(containerMapper.containerToInternalContainerName(container));
        /* check duplicate */
        final Optional<Container> optional = containerRepository.findByInternalName(container.getInternalName());
        if (optional.isPresent()) {
            log.error("Failed to create container with internal name {}", container.getInternalName());
            throw new ContainerAlreadyExistsException("Container name already exists");
        }
        /* create the volume */
        final CreateVolumeResponse response = dockerClient.createVolumeCmd()
                .withName(container.getInternalName())
                .exec();
        log.info("Created volume {} with mapping /var/lib/mysql", response.getName());
        log.trace("created volume {}", response);
        /* create host mapping */
        final HostConfig hostConfig = this.hostConfig
                .withNetworkMode(dockerConfig.getUserNetwork())
                .withBinds(Bind.parse(dockerConfig.getMountPath() + ":/tmp"), Bind.parse(response.getName() + ":/var/lib/mysql"))
                .withPortBindings(PortBinding.parse(availableTcpPort + ":" + image.get().getDefaultPort()));
        log.debug("container has network {}, volume bind {}, volume bind {} and port bind {}",
                dockerConfig.getUserNetwork(), dockerConfig.getMountPath() + ":/tmp",
                response.getName() + ":/var/lib/mysql", availableTcpPort + ":" + image.get().getDefaultPort());
        log.trace("host config {}", hostConfig);
        final User user = userService.findByUsername(principal.getName());
        container.setCreator(user);
        container.setOwner(user);
        /* create the container */
        final CreateContainerResponse response1;
        try {
            response1 = dockerClient.createContainerCmd(
                            containerMapper.containerCreateRequestDtoToDockerImage(createDto))
                    .withName(container.getInternalName())
                    .withHostName(container.getInternalName())
                    .withEnv(imageMapper.environmentItemsToStringList(image.get().getEnvironment()))
                    .withHostConfig(hostConfig)
                    .exec();
        } catch (ConflictException e) {
            log.error("Conflicting names {}, reason: {}", createDto.getName(), e.getMessage());
            throw new ContainerAlreadyExistsException("Conflicting names", e);
        } catch (NotFoundException e) {
            log.error("The image {}:{} not available on the container service", createDto.getRepository(),
                    createDto.getTag());
            throw new DockerClientException("Image not available", e);
        }
        container.setHash(response1.getId());
        container = containerRepository.save(container);
        log.info("Created container {}", container.getId());
        return container;
    }

    @Override
    @Transactional
    public Container stop(Long containerId) throws ContainerNotFoundException,
            ContainerAlreadyStoppedException {
        final Container container = find(containerId);
        final InspectContainerResponse response;
        try {
            response = dockerClient.inspectContainerCmd(container.getHash())
                    .withSize(true)
                    .exec();
            if (response.getState() == null || response.getState().getRunning() == null) {
                log.warn("Failed to determine container state");
            } else if (!response.getState().getRunning()) {
                throw new NotModifiedException("Already stopped");
            }
            dockerClient.stopContainerCmd(container.getHash()).exec();
        } catch (NotFoundException e) {
            log.error("Failed to stop container: {}", e.getMessage());
            throw new ContainerNotFoundException("Failed to stop container: " + e.getMessage(), e);
        } catch (NotModifiedException e) {
            log.warn("Failed to stop container: {}", e.getMessage());
            throw new ContainerAlreadyStoppedException("Failed to stop container: " + e.getMessage(), e);
        }
        log.info("Stopped container with id {}", containerId);
        return container;
    }

    @Override
    @Transactional
    public void remove(Long containerId) throws ContainerNotFoundException,
            ContainerStillRunningException, ContainerAlreadyRemovedException {
        final Container container = find(containerId);
        try {
            dockerClient.removeContainerCmd(container.getHash()).exec();
        } catch (NotFoundException e) {
            log.error("Failed to remove container: {}", e.getMessage());
            throw new ContainerNotFoundException("Failed to remove container", e);
        } catch (NotModifiedException e) {
            log.warn("Failed to remove container: {}", e.getMessage());
            throw new ContainerAlreadyRemovedException("Failed to remove container", e);
        } catch (ConflictException e) {
            log.error("Failed to remove container: {}", e.getMessage());
            throw new ContainerStillRunningException("Failed to remove container", e);
        }
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
    @Transactional
    public Container inspect(Long id) throws ContainerNotFoundException, DockerClientException,
            ContainerNotRunningException {
        final Container container = find(id);
        final InspectContainerResponse response;
        try {
            response = dockerClient.inspectContainerCmd(container.getHash())
                    .withSize(true)
                    .exec();
        } catch (NotFoundException e) {
            log.error("Failed to find container: {}", e.getMessage());
            throw new DockerClientException("Failed to find container", e);
        }
        if (response.getState() == null) {
            log.error("Failed to retrieve container state: is null");
            throw new DockerClientException("Failed to retrieve container state");
        } else if (response.getState().getRunning() == null) {
            log.error("Failed to retrieve container running state: is null");
            throw new DockerClientException("Failed to retrieve container running state");
        }
        if (!response.getState().getRunning()) {
            log.error("Failed to inspect container state: container is not running");
            throw new ContainerNotRunningException("Failed to inspect container state");
        }
        /* now we only support one network */
        response.getNetworkSettings()
                .getNetworks()
                .forEach((key, network) -> {
                    log.trace("key {} network {}", key, network);
                    container.setIpAddress(network.getIpAddress());
                });
        log.info("Inspect container with id {}", id);
        return container;
    }

    @Override
    @Transactional
    public List<Container> getAll() {
        final List<Container> containers = containerRepository.findAll(Sort.by(Sort.Direction.DESC, "created"));
        log.info("Found {} containers", containers.size());
        return containers;
    }

    @Override
    @Transactional
    public Container start(Long containerId) throws ContainerNotFoundException,
            ContainerAlreadyRunningException {
        final Container container = find(containerId);
        final InspectContainerResponse response;
        try {
            response = dockerClient.inspectContainerCmd(container.getHash())
                    .withSize(true)
                    .exec();
            if (response.getState() == null || response.getState().getRunning() == null) {
                log.warn("Failed to determine container state");
            } else if (response.getState().getRunning()) {
                throw new NotModifiedException("Already started");
            }
            dockerClient.startContainerCmd(container.getHash())
                    .exec();
        } catch (NotFoundException e) {
            log.error("Failed to start container, not found: {}", e.getMessage());
            throw new ContainerNotFoundException("Failed to start container: " + e.getMessage(), e);
        } catch (NotModifiedException e) {
            log.warn("Failed to start container, already running: {}", e.getMessage());
            throw new ContainerAlreadyRunningException("Failed to start container: " + e.getMessage(), e);
        }
        log.info("Started container with id {}", containerId);
        return container;
    }

}
