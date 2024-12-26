package at.tuwien.service.impl;

import at.tuwien.config.MetricsConfig;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.MetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class MetricsServicePrometheusImpl implements MetricsService {

    private final MetricsConfig metricsConfig;
    private final MetadataMapper metadataMapper;

    public MetricsServicePrometheusImpl(MetricsConfig metricsConfig, MetadataMapper metadataMapper) {
        this.metricsConfig = metricsConfig;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public void countTableGetData(Long databaseId, Long tableId) {
        countGetData(databaseId, tableId, null, null);
    }

    @Override
    public void countSubsetGetData(Long databaseId, Long subsetId) {
        countGetData(databaseId, null, subsetId, null);
    }

    @Override
    public void countViewGetData(Long databaseId, Long viewId) {
        countGetData(databaseId, null, null, viewId);
    }

    public void countGetData(Long databaseId, Long tableId, Long subsetId, Long viewId) {
        Counter.builder("dbrepo.datasource.data.get")
                .tag("uri", metadataMapper.metricToUri(metricsConfig.getBaseUrl(), databaseId, tableId, subsetId, viewId))
                .tag("protocol", "http")
                .description("The total number of accessed data sources")
                .register(Metrics.globalRegistry)
                .increment();
    }

}
