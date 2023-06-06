package at.tuwien.service.impl;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.repository.mdb.AccessRepository;
import at.tuwien.service.AccessService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class AccessServiceImpl implements AccessService {

    private final AccessRepository accessRepository;

    @Autowired
    public AccessServiceImpl(AccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }

    @Override
    public DatabaseAccess find(Long databaseId, UUID userId) throws AccessDeniedException {
        final Optional<DatabaseAccess> optional = accessRepository.findByHdbidAndHuserid(databaseId, userId);
        if (optional.isEmpty()) {
            log.error("Failed to find access for user with id {}", userId);
            throw new AccessDeniedException("Failed to find access");
        }
        return optional.get();
    }
}
