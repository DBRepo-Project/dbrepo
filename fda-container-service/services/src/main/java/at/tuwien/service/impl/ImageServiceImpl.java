package at.tuwien.service.impl;

import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageEnvItemDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.exception.*;
import at.tuwien.mapper.ImageMapper;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.ImageService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    private final ImageMapper imageMapper;
    private final DockerClient dockerClient;
    private final ImageRepository imageRepository;

    @Autowired
    public ImageServiceImpl(DockerClient dockerClient, ImageRepository imageRepository, ImageMapper imageMapper) {
        this.dockerClient = dockerClient;
        this.imageRepository = imageRepository;
        this.imageMapper = imageMapper;
    }

    @Override
    @Transactional
    public List<ContainerImage> getAll() {
        return imageRepository.findAll();
    }

    @Override
    @Transactional
    public ContainerImage find(Long imageId) throws ImageNotFoundException {
        final Optional<ContainerImage> image = imageRepository.findById(imageId);
        if (image.isEmpty()) {
            log.error("Failed to find image with id {} in metadata database", imageId);
            throw new ImageNotFoundException("Failed to find image");
        }
        return image.get();
    }

    @Override
    @Transactional
    public ContainerImage create(ImageCreateDto createDto, Principal principal) throws ImageNotFoundException,
            ImageAlreadyExistsException,
            DockerClientException, UserNotFoundException {
        final ContainerImage image = inspect(createDto.getRepository(), createDto.getTag());
        if (imageRepository.findByRepositoryAndTag(createDto.getRepository(), createDto.getTag()).isPresent()) {
            log.error("Failed to create image {}:{}, it already exists in the metadata database",
                    createDto.getRepository(), createDto.getTag());
            throw new ImageAlreadyExistsException("Failed to create image");
        }
        image.setEnvironment(imageMapper.imageEnvironmentItemDtoToEnvironmentItemList(createDto.getEnvironment()));
        image.setDefaultPort(createDto.getDefaultPort());
        image.setDialect(createDto.getDialect());
        image.setDriverClass(createDto.getDriverClass());
        image.setJdbcMethod(createDto.getJdbcMethod());
        final ContainerImage dto;
        try {
            dto = imageRepository.save(image);
        } catch (ConstraintViolationException | DataIntegrityViolationException e) {
            log.error("Failed to create image: {}", e.getMessage());
            throw new ImageAlreadyExistsException("Failed to create image", e);
        }
        log.info("Created image {}", dto.getId());
        log.trace("created image {}", dto);
        return dto;
    }

    @Override
    @Transactional
    public ContainerImage update(Long imageId, ImageChangeDto changeDto) throws ImageNotFoundException {
        final ContainerImage image = find(imageId);
        /* pull changes */
        pull(image.getRepository(), image.getTag());
        /* get new infos */
        final ContainerImage dockerImage = inspect(image.getRepository(), image.getTag());
        if (!changeDto.getDefaultPort().equals(image.getDefaultPort())) {
            image.setDefaultPort(changeDto.getDefaultPort());
            log.debug("default port changed from {} to {} for image with id {}", image.getDefaultPort(),
                    changeDto.getDefaultPort(), imageId);
        }
        final List<ContainerImageEnvironmentItem> env = imageMapper.imageEnvironmentItemDtoToEnvironmentItemList(changeDto.getEnvironment());
        if (env.equals(image.getEnvironment())) {
            image.setEnvironment(env);
            log.debug("environment changed for image with id {}", imageId);
            log.trace("environment changed from {} to {} for image with id {}", env, image.getEnvironment(), imageId);
        }
        image.setCompiled(dockerImage.getCompiled());
        image.setHash(dockerImage.getHash());
        image.setSize(dockerImage.getSize());
        image.setDialect(changeDto.getDialect());
        image.setDriverClass(changeDto.getDriverClass());
        image.setJdbcMethod(changeDto.getJdbcMethod());
        /* update metadata db */
        final ContainerImage out = imageRepository.save(image);
        log.info("Updated image {}", out.getId());
        log.trace("updated image {}", out);
        return out;
    }

    @Override
    @Transactional
    public void delete(Long imageId) throws ImageNotFoundException, PersistenceException {
        try {
            imageRepository.deleteById(imageId);
        } catch (EntityNotFoundException | EmptyResultDataAccessException e) {
            log.error("Failed to delete image with id {}, reason: {}", imageId, e.getMessage());
            throw new ImageNotFoundException("Failed to delete image", e);
        } catch (ConstraintViolationException e) {
            log.error("Failed to delete image with id {} with constraint, reason: {}", imageId, e.getMessage());
            throw new ImageNotFoundException("Failed to delete image with constraint", e);
        }
        log.info("Deleted image {}", imageId);
    }

    /**
     * Inspects a container image by given repository and tag.
     *
     * @param repository The repository.
     * @param tag        The tag.
     * @return The container image if successful.
     * @throws ImageNotFoundException The image was not found.
     */
    public ContainerImage inspect(String repository, String tag) throws ImageNotFoundException {
        final InspectImageResponse response;
        try {
            response = dockerClient.inspectImageCmd(repository + ":" + tag)
                    .exec();
            log.trace("inspected image {}", response);
        } catch (NotFoundException e) {
            log.error("Failed to find image {}:{}, reason: {}", repository, tag, e.getMessage());
            throw new ImageNotFoundException("Failed to find image", e);
        }
        return imageMapper.inspectImageResponseToContainerImage(response);
    }

    @Override
    public boolean exists(String repository, String tag) {
        final List<Image> images = dockerClient.listImagesCmd()
                .exec();
        log.trace("found images {}", images);
        return images.stream()
                .filter(i -> Objects.nonNull(i.getRepoTags()))
                .filter(i -> i.getRepoTags().length > 0)
                .anyMatch(i -> Arrays.stream(i.getRepoTags())
                        .anyMatch(t -> t.equals(repository + ":" + tag)));
    }

    @Override
    public void pull(String repository, String tag) throws ImageNotFoundException {
        log.debug("pulling image {}:{}", repository, tag);
        final ResultCallback.Adapter<PullResponseItem> response;
        try {
            response = dockerClient.pullImageCmd(repository)
                    .withTag(tag)
                    .start();
            log.trace("pulled image {}", response);
            final Instant now = Instant.now();
            response.awaitCompletion();
            log.info("Pulled image {}:{}", repository, tag);
            log.debug("pulled image {}:{} in {} seconds", repository, tag, Duration.between(now, Instant.now()).getSeconds());
        } catch (NotFoundException | InternalServerErrorException e) {
            log.warn("Failed to pull image {}:{}, reason: {}", repository, tag, e.getMessage());
            throw new ImageNotFoundException("Failed to pull image", e);
        } catch (InterruptedException e) {
            log.error("Failed to pull image {}:{} un-interrupted: {}", repository, tag, e.getMessage());
            throw new ImageNotFoundException("failed to pull image", e);
        }
    }

}
