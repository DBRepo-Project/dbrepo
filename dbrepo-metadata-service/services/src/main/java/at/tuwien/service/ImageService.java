package at.tuwien.service;

import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.exception.ImageInvalidException;
import at.tuwien.exception.ImageNotFoundException;

import java.security.Principal;
import java.util.List;

public interface ImageService {

    /**
     * Finds all container images in the metadata database.
     *
     * @return List of container images
     */
    List<ContainerImage> getAll();

    /**
     * Finds a specific container image by given id.
     *
     * @param imageId The image id.
     * @return The image, if successful.
     */
    ContainerImage find(Long imageId) throws ImageNotFoundException;

    /**
     * Creates a new container image in the metadata database.
     *
     * @param createDto The new image.
     * @param principal The user principal.
     * @return The container image, if successful.
     * @throws ImageAlreadyExistsException The image already exists.
     * @throws ImageInvalidException       The default image cannot be created as a default image already exists.
     */
    ContainerImage create(ImageCreateDto createDto, Principal principal) throws ImageAlreadyExistsException,
            ImageInvalidException;

    /**
     * Updates a container image with given id in the metadata database.
     *
     * @param image     The image.
     * @param changeDto The update request.
     * @return The updated container image, if successful.
     */
    ContainerImage update(ContainerImage image, ImageChangeDto changeDto);

    /**
     * Deletes a container image with given id in the metadata database.
     *
     * @param image The image.
     */
    void delete(ContainerImage image);
}
