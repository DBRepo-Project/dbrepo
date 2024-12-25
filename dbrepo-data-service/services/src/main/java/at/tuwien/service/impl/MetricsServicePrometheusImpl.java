package at.tuwien.service.impl;

import at.tuwien.service.MetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class MetricsServicePrometheusImpl implements MetricsService {

    @Override
    public void countDatabaseGetData(Long databaseId) {
        Counter.builder("dbrepo.database.data.get")
                .tag("database_id", "" + databaseId)
                .tag("protocol", "http")
                .description("The total number of accessed data sources")
                .register(Metrics.globalRegistry)
                .increment();
    }

    @Override
    public void countTableGetData(Long databaseId, Long tableId) {
        Counter.builder("dbrepo.table.data.get")
                .tag("database_id", "" + databaseId)
                .tag("table_id", "" + tableId)
                .tag("protocol", "http")
                .description("The total number of accessed data sources")
                .register(Metrics.globalRegistry)
                .increment();
    }

    @Override
    public void countSubsetGetData(Long databaseId, Long subsetId) {
        Counter.builder("dbrepo.table.data.get")
                .tag("database_id", "" + databaseId)
                .tag("subset_id", "" + subsetId)
                .tag("protocol", "http")
                .description("The total number of accessed data sources")
                .register(Metrics.globalRegistry)
                .increment();
    }

    @Override
    public void countViewGetData(Long databaseId, Long viewId) {
        Counter.builder("dbrepo.table.data.get")
                .tag("database_id", "" + databaseId)
                .tag("view_id", "" + viewId)
                .tag("protocol", "http")
                .description("The total number of accessed data sources")
                .register(Metrics.globalRegistry)
                .increment();
    }

}
