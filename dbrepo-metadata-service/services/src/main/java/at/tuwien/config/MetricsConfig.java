package at.tuwien.config;

import at.tuwien.entities.database.table.Table;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.repository.IdentifierRepository;
import at.tuwien.repository.TableRepository;
import at.tuwien.repository.ViewRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
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
        return Gauge.builder("dbrepo.database.count", () -> databaseRepository.findAll().size())
                .description("The total number of managed research databases")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge viewCountGauge() {
        return Gauge.builder("dbrepo.view.count", () -> viewRepository.findAll().size())
                .description("The total number of available view data sources")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge subsetCountGauge() {
        return Gauge.builder("dbrepo.subset.count", () -> identifierRepository.findAllSubsetIdentifiers().size())
                .description("The total number of available subset data sources")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge tableCountGauge() {
        return Gauge.builder("dbrepo.table.count", () -> tableRepository.findAll().size())
                .description("The total number of available table data sources")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Gauge volumeSumGauge() {
        return Gauge.builder("dbrepo.volume.sum", () -> {
                    if (tableRepository.findAll().isEmpty()) {
                        return 0;
                    }
                    return tableRepository.findAll().stream().map(Table::getDataLength).mapToLong(d -> d).sum();
                })
                .description("The total volume of available research data")
                .strongReference(true)
                .register(Metrics.globalRegistry);
    }

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
