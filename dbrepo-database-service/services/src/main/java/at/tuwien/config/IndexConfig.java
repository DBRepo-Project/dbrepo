package at.tuwien.config;

import at.tuwien.entities.database.Database;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.repository.mdb.DatabaseRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Component
public class IndexConfig {

    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    public IndexConfig(DatabaseRepository databaseRepository, DatabaseIdxRepository databaseIdxRepository) {
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final List<Database> databases = databaseRepository.findAll();
        databaseIdxRepository.saveAll(databases);
        log.info("Added {} databases to open search index", databases.size());
    }

}
