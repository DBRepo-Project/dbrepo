package at.tuwien.service;

import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

public interface ImageService {

    /**
     * Finds all container images in the metadata database
     *
     * @return A list of container images
     */
    List<ContainerImage> getAll();

    /**
     * Finds a specific container image by given id
     *
     * @param imageId The id.
     * @return The image, if found.
     * @throws ImageNotFoundException The image was not found
     */
    ContainerImage find(Long imageId) throws ImageNotFoundException;

    /**
     * Creates a new container image in the metadata database.
     *
     * @param createDto The new image.
     * @return The created container image, if successful.
     * @throws ImageNotFoundException      The image was not found in the remote repository (e.g. Docker Registry)
     * @throws ImageAlreadyExistsException The image already exists.
     * @throws DockerClientException       The docker client encountered a problem.
     */
    ContainerImage create(ImageCreateDto createDto, Principal principal) throws ImageNotFoundException,
            ImageAlreadyExistsException,
            DockerClientException, UserNotFoundException;

    /**
     * Updates a container image in the metadata database by given id.
     *
     * @param imageId   The id.
     * @param changeDto The update request.
     * @return The updated container image, if successful.
     * @throws ImageNotFoundException The image was not found in the metadata database.
     * @throws DockerClientException  The docker client encountered a problem.
     */
    ContainerImage update(Long imageId, ImageChangeDto changeDto) throws ImageNotFoundException, DockerClientException;

    /**
     * Deletes a container image in the metadata database by given id.
     *
     * @param id The id.
     * @throws ImageNotFoundException The image was not found.
     * @throws PersistenceException   The database returned an error.
     */
    void delete(Long id) throws ImageNotFoundException, PersistenceException;

    /**
     * Checks if an image exists locally.
     *
     * @param repository The image name.
     * @param tag        The image tag.
     * @return True if the image exists, false otherwise.
     */
    boolean exists(String repository, String tag);

    /**
     * Pulls a container image by given repository and tag.
     *
     * @param repository The repository.
     * @param tag        The tag.
     * @throws ImageNotFoundException The image was not found.
     */
    void pull(String repository, String tag) throws ImageNotFoundException;
}
