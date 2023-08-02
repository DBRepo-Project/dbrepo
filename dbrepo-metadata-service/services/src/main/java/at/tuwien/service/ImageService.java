package at.tuwien.service;

import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.exception.PersistenceException;
import at.tuwien.exception.UserNotFoundException;

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
     * @throws ImageNotFoundException The image was not found in the metadata database.
     */
    ContainerImage find(Long imageId) throws ImageNotFoundException;

    /**
     * Creates a new container image in the metadata database.
     *
     * @param createDto The new image.
     * @param principal The user principal.
     * @return The container image, if successful.
     * @throws ImageNotFoundException      The image was not found.
     * @throws ImageAlreadyExistsException An image with this repository name and tag already exists.
     * @throws UserNotFoundException       The user could not be found by the user principal.
     */
    ContainerImage create(ImageCreateDto createDto, Principal principal) throws ImageNotFoundException,
            ImageAlreadyExistsException, UserNotFoundException;

    /**
     * Updates a container image with given id in the metadata database.
     *
     * @param imageId   The image id.
     * @param changeDto The update request.
     * @return The updated container image, if successful.
     * @throws ImageNotFoundException The image was not found in the metadata database.
     */
    ContainerImage update(Long imageId, ImageChangeDto changeDto) throws ImageNotFoundException;

    /**
     * Deletes a container image with given id in the metadata database.
     *
     * @param imageId The image id.
     * @throws ImageNotFoundException The image was not found.
     * @throws PersistenceException   The database returned an error.
     */
    void delete(Long imageId) throws ImageNotFoundException, PersistenceException;
}
