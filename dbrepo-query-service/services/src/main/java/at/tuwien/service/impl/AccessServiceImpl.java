package at.tuwien.service.impl;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.AccessService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public DatabaseAccess find(Long databaseId, String username) throws NotAllowedException {
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username);
        if (optional.isEmpty()) {
            log.error("Failed to find database access for database with id {}", databaseId);
            throw new NotAllowedException("Failed to find database access");
        }
        return optional.get();
    }

}
