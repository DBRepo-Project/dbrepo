package at.tuwien.service.impl;

import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.mapper.ImageMapper;
import at.tuwien.repository.mdb.ImageRepository;
import at.tuwien.service.ImageService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    private final ImageMapper imageMapper;
    private final ImageRepository imageRepository;

    @Autowired
    public ImageServiceImpl(ImageRepository imageRepository, ImageMapper imageMapper) {
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
            throw new ImageNotFoundException("Failed to find image with id " + imageId + " in metadata database");
        }
        return image.get();
    }

    @Override
    @Transactional
    public ContainerImage create(ImageCreateDto createDto, Principal principal) throws ImageAlreadyExistsException {
        final ContainerImage image = imageMapper.createImageDtoToContainerImage(createDto);
        if (imageRepository.findByNameAndVersion(createDto.getName(), createDto.getVersion()).isPresent()) {
            log.error("Failed to create image {}:{}: exists in the metadata database",
                    createDto.getName(), createDto.getVersion());
            throw new ImageAlreadyExistsException("Failed to create image " + createDto.getName() + ":" + createDto.getVersion() + ": exists in the metadata database");
        }
        final ContainerImage dto;
        try {
            dto = imageRepository.save(image);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            log.error("Failed to create image: {}", e.getMessage());
            throw new ImageAlreadyExistsException("Failed to create image", e);
        }
        log.info("Created image with id {} in metadata database", dto.getId());
        return dto;
    }

    @Override
    @Transactional
    public ContainerImage update(Long imageId, ImageChangeDto changeDto) throws ImageNotFoundException {
        final ContainerImage image = find(imageId);
        if (!changeDto.getDefaultPort().equals(image.getDefaultPort())) {
            image.setDefaultPort(changeDto.getDefaultPort());
            log.debug("default port changed from {} to {} for image with id {}", image.getDefaultPort(),
                    changeDto.getDefaultPort(), imageId);
        }
        image.setDialect(changeDto.getDialect());
        image.setDriverClass(changeDto.getDriverClass());
        image.setJdbcMethod(changeDto.getJdbcMethod());
        /* update metadata db */
        final ContainerImage out = imageRepository.save(image);
        log.info("Updated image with id {} in metadata database", out.getId());
        return out;
    }

    @Override
    @Transactional
    public void delete(Long imageId) throws ImageNotFoundException {
        find(imageId);
        try {
            imageRepository.deleteById(imageId);
            log.info("Deleted image with id {} in metadata database", imageId);
        } catch (EntityNotFoundException | EmptyResultDataAccessException | DataIntegrityViolationException e) {
            log.error("Failed to delete image with id {} with constraint: {}", imageId, e.getMessage());
            throw new ImageNotFoundException("Failed to delete image with id " + imageId + " with constraint", e);
        }
    }

}
