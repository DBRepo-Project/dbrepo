package at.tuwien.service.impl;

import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.exception.ImageInvalidException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.repository.ImageRepository;
import at.tuwien.service.ImageService;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    private final MetadataMapper metadataMapper;
    private final ImageRepository imageRepository;

    @Autowired
    public ImageServiceImpl(ImageRepository imageRepository, MetadataMapper metadataMapper) {
        this.imageRepository = imageRepository;
        this.metadataMapper = metadataMapper;
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
            log.error("Failed to find image with id {}", imageId);
            throw new ImageNotFoundException("Failed to find image with id " + imageId);
        }
        return image.get();
    }

    @Override
    @Transactional
    public ContainerImage create(ImageCreateDto createDto, Principal principal) throws ImageAlreadyExistsException,
            ImageInvalidException {
        final ContainerImage image = metadataMapper.createImageDtoToContainerImage(createDto);
        if (imageRepository.findByNameAndVersion(createDto.getName(), createDto.getVersion()).isPresent()) {
            log.error("Failed to create image {}:{}: exists in the metadata database", createDto.getName(), createDto.getVersion());
            throw new ImageAlreadyExistsException("Failed to create image " + createDto.getName() + ":" + createDto.getVersion() + ": exists in the metadata database");
        }
        if (createDto.getIsDefault() && imageRepository.findByIsDefault(true).isPresent()) {
            log.error("Failed to create image {}:{}: default image exists", createDto.getName(), createDto.getVersion());
            throw new ImageInvalidException("Failed to create image: default image exists");
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
    public ContainerImage update(ContainerImage image, ImageChangeDto changeDto) {
        if (!changeDto.getDefaultPort().equals(image.getDefaultPort())) {
            image.setDefaultPort(changeDto.getDefaultPort());
            log.debug("default port changed from {} to {} for image with id {}", image.getDefaultPort(),
                    changeDto.getDefaultPort(), image.getId());
        }
        image.setDialect(changeDto.getDialect());
        image.setDriverClass(changeDto.getDriverClass());
        image.setJdbcMethod(changeDto.getJdbcMethod());
        /* update metadata db */
        image = imageRepository.save(image);
        log.info("Updated image with id {} in metadata database", image.getId());
        return image;
    }

    @Override
    @Transactional
    public void delete(ContainerImage image) {
        imageRepository.deleteById(image.getId());
        log.info("Deleted image with id {} in metadata database", image.getId());
    }

}
