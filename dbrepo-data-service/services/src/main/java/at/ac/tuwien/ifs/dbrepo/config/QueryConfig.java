package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.jooq.impl.DSL.using;

@Log4j2
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
