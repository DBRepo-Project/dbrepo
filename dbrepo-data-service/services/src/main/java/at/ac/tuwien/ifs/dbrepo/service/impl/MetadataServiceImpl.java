package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.*;
import at.ac.tuwien.ifs.dbrepo.config.CacheConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    private final CacheConfig cacheConfig;
    private final UserCacheRepository userRepository;
    private final ViewCacheRepository viewRepository;
    private final ImageCacheRepository imageRepository;
    private final TableCacheRepository tableRepository;
    private final DatabaseCacheRepository databaseRepository;
    private final ContainerCacheRepository containerRepository;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public MetadataServiceImpl(CacheConfig cacheConfig, UserCacheRepository userRepository,
                               ViewCacheRepository viewRepository, ImageCacheRepository imageRepository,
                               TableCacheRepository tableRepository, DatabaseCacheRepository databaseRepository,
                               ContainerCacheRepository containerRepository,
                               MetadataServiceGateway metadataServiceGateway) {
        this.cacheConfig = cacheConfig;
        this.userRepository = userRepository;
        this.viewRepository = viewRepository;
        this.imageRepository = imageRepository;
        this.tableRepository = tableRepository;
        this.databaseRepository = databaseRepository;
        this.containerRepository = containerRepository;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @Override
    public Database getDatabase(UUID id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final Optional<Database> optional = databaseRepository.findById(id);
        if (optional.isPresent()) {
            log.trace("Cache hit on database: {}", id);
            return optional.get();
        }
        log.trace("Cache miss on database: {}", id);
        final Database database = metadataServiceGateway.getDatabaseById(id);
        database.setExp(cacheConfig.getTtl());
        return databaseRepository.save(database);
    }

    @Override
    public User getUser(String username) throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException {
        final Optional<User> optional = userRepository.findById(username);
        if (optional.isPresent()) {
            log.trace("Cache hit on user: {}", username);
            return optional.get();
        }
        log.trace("Cache miss on user: {}", username);
        final User user = metadataServiceGateway.getUserByUsername(username);
        user.setExp(cacheConfig.getTtl());
        return userRepository.save(user);
    }

    @Override
    public Image getImage(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ImageNotFoundException {
        final Optional<Image> optional = imageRepository.findById(id);
        if (optional.isPresent()) {
            log.trace("Cache hit on image: {}", id);
            return optional.get();
        }
        log.trace("Cache miss on image: {}", id);
        final Image image = metadataServiceGateway.getImageById(id);
        image.setExp(cacheConfig.getTtl());
        return imageRepository.save(image);
    }

    @Override
    public Container getContainer(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException {
        final Optional<Container> optional = containerRepository.findById(id);
        if (optional.isPresent()) {
            log.trace("Cache hit on container: {}", id);
            return optional.get();
        }
        log.trace("Cache miss on container: {}", id);
        final Container container = metadataServiceGateway.getContainerById(id);
        container.setExp(cacheConfig.getTtl());
        return containerRepository.save(container);
    }

    @Override
    public Table getTable(UUID databaseId, UUID id) throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final Optional<Table> optional = tableRepository.findById(id);
        if (optional.isPresent()) {
            log.trace("Cache hit on table: {}", id);
            return optional.get();
        }
        log.trace("Cache miss on table: {}", id);
        final Table table = metadataServiceGateway.getTableById(databaseId, id);
        table.setExp(cacheConfig.getTtl());
        return tableRepository.save(table);
    }

    @Override
    public View getView(UUID databaseId, UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ViewNotFoundException {
        final Optional<View> optional = viewRepository.findById(id);
        if (optional.isPresent()) {
            log.trace("Cache hit on view: {}", id);
            return optional.get();
        }
        log.trace("Cache miss on view: {}", id);
        final View view = metadataServiceGateway.getViewById(databaseId, id);
        view.setExp(cacheConfig.getTtl());
        return viewRepository.save(view);
    }
}
