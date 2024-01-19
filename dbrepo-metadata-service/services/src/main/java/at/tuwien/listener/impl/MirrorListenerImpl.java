package at.tuwien.listener.impl;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.listener.MirrorListener;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class MirrorListenerImpl implements MirrorListener {

    private final DatabaseMapper databaseMapper;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public MirrorListenerImpl(DatabaseMapper databaseMapper, DatabaseRepository databaseRepository,
                              DatabaseIdxRepository databaseIdxRepository) {
        this.databaseMapper = databaseMapper;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Override
    @Scheduled(fixedRateString = "${fda.mirrorRate}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void mirrorEntities() {
        final List<DatabaseDto> databases = databaseRepository.findAll()
                .stream()
                .map(databaseMapper::databaseToDatabaseDto)
                .toList();
        databaseIdxRepository.saveAll(databases);
        log.info("Updated {} databases", databases.size());
    }
}
