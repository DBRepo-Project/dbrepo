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
     * @throws ImageNotFoundException      The image was not found in the docker.io registry.
     * @throws ImageAlreadyExistsException An image with this repository name and tag already exists.
     * @throws DockerClientException       The image could not be created due to the Docker daemon refusing to create it.
     * @throws UserNotFoundException       The user could not be found by the user principal.
     */
    ContainerImage create(ImageCreateDto createDto, Principal principal) throws ImageNotFoundException,
            ImageAlreadyExistsException, DockerClientException, UserNotFoundException;

    /**
     * Updates a container image with given id in the metadata database and pull an updated docker image from the docker.io repository.
     *
     * @param imageId   The image id.
     * @param changeDto The update request.
     * @return The updated container image, if successful.
     * @throws ImageNotFoundException The image was not found in the metadata database.
     * @throws DockerClientException  The image could not be updated due to the Docker daemon refusing to pull it.
     */
    ContainerImage update(Long imageId, ImageChangeDto changeDto) throws ImageNotFoundException, DockerClientException;

    /**
     * Deletes a container image with given id in the metadata database.
     *
     * @param imageId The image id.
     * @throws ImageNotFoundException The image was not found.
     * @throws PersistenceException   The database returned an error.
     */
    void delete(Long imageId) throws ImageNotFoundException, PersistenceException;

    /**
     * Checks if an image exists locally.
     *
     * @param repository The image repository.
     * @param tag        The image tag.
     * @return True if the image exists, false otherwise.
     */
    boolean exists(String repository, String tag);

    /**
     * Pulls a container image by given registry, repository and tag.
     *
     * @param registry   The registry.
     * @param repository The repository.
     * @param tag        The tag.
     * @throws ImageNotFoundException The image was not found.
     */
    void pull(String registry, String repository, String tag) throws ImageNotFoundException;
}
