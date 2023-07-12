package at.tuwien.config;

import at.tuwien.api.database.ViewDto;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.sdb.ViewIdxRepository;
import at.tuwien.repository.mdb.ViewRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Component
public class IndexConfig {

    private final ViewMapper viewMapper;
    private final ViewRepository viewRepository;
    private final ViewIdxRepository viewIdxRepository;

    @Autowired
    public IndexConfig(ViewMapper viewMapper, ViewRepository viewRepository, ViewIdxRepository viewIdxRepository) {
        this.viewMapper = viewMapper;
        this.viewRepository = viewRepository;
        this.viewIdxRepository = viewIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final List<ViewDto> views = viewRepository.findAll()
                .stream()
                .map(viewMapper::viewToViewDto)
                .toList();
        viewIdxRepository.saveAll(views);
        log.info("Added {} views to open search database", views.size());
    }
}
