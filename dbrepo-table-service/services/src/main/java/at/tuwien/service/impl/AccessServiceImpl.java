package at.tuwien.service.impl;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.AccessService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Log4j2
@Service
public class AccessServiceImpl implements AccessService {

    private final DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    public AccessServiceImpl(DatabaseAccessRepository databaseAccessRepository) {
        this.databaseAccessRepository = databaseAccessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseAccess hasAccess(Long databaseId, Long tableId, String username) throws AccessDeniedException {
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username);
        if (optional.isEmpty()) {
            log.error("Failed to retrieve access, not found");
            throw new AccessDeniedException("Failed to retrieve access");
        }
        return optional.get();
    }

}
