package at.tuwien.config;

import at.tuwien.api.database.ViewDto;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.elastic.ViewIdxRepository;
import at.tuwien.repository.jpa.ViewRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class IndexConfig {

    private final ViewMapper viewMapper;
    private final ViewRepository viewRepository;
    private final ViewIdxRepository viewIdxRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public IndexConfig(ViewMapper viewMapper, ViewRepository viewRepository, ViewIdxRepository viewIdxRepository,
                       ElasticsearchOperations elasticsearchOperations) {
        this.viewMapper = viewMapper;
        this.viewRepository = viewRepository;
        this.viewIdxRepository = viewIdxRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final IndexCoordinates viewIndex = IndexCoordinates.of("view");
        if (!elasticsearchOperations.indexOps(viewIndex).exists()) {
            elasticsearchOperations.indexOps(viewIndex).create();
            elasticsearchOperations.indexOps(viewIndex).createMapping(ViewDto.class);
            log.info("Created identifier index");
        }
        /* pre-fill */
        final List<ViewDto> views = viewRepository.findAll()
                .stream()
                .map(viewMapper::viewToViewDto)
                .collect(Collectors.toList());
        log.info("Added {} views to OpenSearch index", views.size());
        viewIdxRepository.saveAll(views);
    }
}
