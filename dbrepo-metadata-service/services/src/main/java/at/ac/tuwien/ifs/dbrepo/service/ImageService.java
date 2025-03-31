package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageChangeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageInvalidException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

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
     * @throws ImageNotFoundException The image was not found in the metadata service.
     */
    ContainerImage find(UUID imageId) throws ImageNotFoundException;

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
