package at.tuwien.service;

import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

public interface ContainerService {

    /**
     * @param createDto
     * @param principal
     * @return
     * @throws ImageNotFoundException
     * @throws DockerClientException
     * @throws ContainerAlreadyExistsException
     * @throws UserNotFoundException
     */
    Container create(ContainerCreateRequestDto createDto, Principal principal) throws ImageNotFoundException,
            DockerClientException, ContainerAlreadyExistsException, UserNotFoundException;

    /**
     * @param containerId
     * @return
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     */
    Container stop(Long containerId) throws ContainerNotFoundException, DockerClientException;

    /**
     * @param containerId
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     * @throws ContainerStillRunningException
     */
    void remove(Long containerId) throws ContainerNotFoundException, DockerClientException,
            ContainerStillRunningException;

    /**
     * @param id
     * @return
     * @throws ContainerNotFoundException
     */
    Container find(Long id) throws ContainerNotFoundException;

    /**
     * @param id
     * @return
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     * @throws ContainerNotRunningException
     */
    Container inspect(Long id) throws ContainerNotFoundException, DockerClientException, ContainerNotRunningException;

    /**
     * @return
     */
    List<Container> getAll();

    /**
     * @param containerId
     * @return
     * @throws ContainerNotFoundException
     * @throws DockerClientException
     */
    Container start(Long containerId) throws ContainerNotFoundException, DockerClientException;
}
