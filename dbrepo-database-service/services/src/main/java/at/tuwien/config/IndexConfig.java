package at.tuwien.config;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.mapper.DatabaseMapper;
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

    private final DatabaseMapper databaseMapper;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    public IndexConfig(DatabaseMapper databaseMapper, DatabaseRepository databaseRepository,
                       DatabaseIdxRepository databaseIdxRepository) {
        this.databaseMapper = databaseMapper;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final List<DatabaseDto> databases = databaseRepository.findAll()
                .stream()
                .map(databaseMapper::databaseToDatabaseDto)
                .toList();
        databaseIdxRepository.saveAll(databases);
        log.info("Added {} databases to open search index", databases.size());
    }

}
