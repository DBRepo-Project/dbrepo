package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierType;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.IdentifierRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.TableRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.ViewRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MetricsConfig {

    private final ViewRepository viewRepository;
    private final TableRepository tableRepository;
    private final DatabaseRepository databaseRepository;
    private final IdentifierRepository identifierRepository;

    @Autowired
    public MetricsConfig(ViewRepository viewRepository, TableRepository tableRepository,
                         DatabaseRepository databaseRepository, IdentifierRepository identifierRepository) {
        this.viewRepository = viewRepository;
        this.tableRepository = tableRepository;
        this.databaseRepository = databaseRepository;
        this.identifierRepository = identifierRepository;
    }

    @Bean
    public Gauge databaseCountGauge() {
        return Gauge.builder("dbrepo.database.count", databaseRepository::count)
                .description("The total number of managed research databases")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge viewCountGauge() {
        return Gauge.builder("dbrepo.view.count", viewRepository::count)
                .description("The total number of available view data sources")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge subsetCountGauge() {
        return Gauge.builder("dbrepo.subset.count", () -> identifierRepository.countByType(IdentifierType.SUBSET))
                .description("The total number of available subset data sources")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge tableCountGauge() {
        return Gauge.builder("dbrepo.table.count", tableRepository::count)
                .description("The total number of available table data sources")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge volumeSumGauge() {
        return Gauge.builder("dbrepo.volume.sum", tableRepository::sumDataLength)
                .description("The total volume of available research data")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
