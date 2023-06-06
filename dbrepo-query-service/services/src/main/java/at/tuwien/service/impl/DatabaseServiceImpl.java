package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.DatabaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class DatabaseServiceImpl implements DatabaseService {

    private final DatabaseRepository databaseRepository;

    @Autowired
    public DatabaseServiceImpl(DatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Database find(Long containerId, Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findByContainerIdAndDatabaseId(containerId, databaseId);
        if (database.isEmpty()) {
            log.error("Failed to find database with container id {} and database id {}", containerId, databaseId);
            throw new DatabaseNotFoundException("Failed to find database");
        }
        return database.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Database> findAll() {
        return databaseRepository.findAll();
    }
}
