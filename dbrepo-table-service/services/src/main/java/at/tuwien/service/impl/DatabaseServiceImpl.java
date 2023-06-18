package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.DatabaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public Database find(Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findByContainerIdAndDatabaseId(databaseId);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {}", databaseId);
            throw new DatabaseNotFoundException("could not find database with this id");
        }
        return database.get();
    }
}
