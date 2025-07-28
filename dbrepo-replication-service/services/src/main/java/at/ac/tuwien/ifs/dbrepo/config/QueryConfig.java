package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.jooq.impl.DSL.using;

@Slf4j
@Getter
@Configuration
public class QueryConfig {

    @Bean
    public DSLContext context() {
        final DefaultConfiguration configuration = new DefaultConfiguration();
        final Settings settings = new Settings();
        settings.setStatementType(StatementType.STATIC_STATEMENT);
        configuration.setSettings(settings);
        configuration.set(SQLDialect.MARIADB);
        return using(configuration);
    }

}
