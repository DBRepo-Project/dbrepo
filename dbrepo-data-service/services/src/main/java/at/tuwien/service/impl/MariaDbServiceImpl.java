package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.DatabaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class MariaDbServiceImpl extends HibernateConnector implements DatabaseService {

    private final DatabaseRepository databaseRepository;

    @Autowired
    public MariaDbServiceImpl(DatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public List<Database> findAll() {
        return databaseRepository.findAll();
    }

    @Override
    public Database find(Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(databaseId);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {}", databaseId);
            throw new DatabaseNotFoundException("could not find database with this id");
        }
        return database.get();
    }

}
