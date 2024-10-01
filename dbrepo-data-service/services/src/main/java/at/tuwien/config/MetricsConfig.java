package at.tuwien.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    @Bean
    public Counter httpDataAccessCounter() {
        return Counter.builder("dbrepo.data.access")
                .tag("protocol", "http")
                .description("The total number of accessed data sources")
                .register(Metrics.globalRegistry);
    }

    @Bean
    public Counter amqpDataAccessCounter() {
        return Counter.builder("dbrepo.data.access")
                .tag("protocol", "amqp")
                .description("The total number of accessed data sources")
                .register(Metrics.globalRegistry);
    }
}
