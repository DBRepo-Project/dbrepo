package at.tuwien.service.impl;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.DatabaseConnectionException;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Collectors;

@Log4j2
@Service
public abstract class HibernateConnector {

    protected static ComboPooledDataSource getDataSource(ContainerImage image, Container container, Database database,
                                                         User user) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        final String url = "jdbc:" + image.getJdbcMethod() + "://" + container.getInternalName() + "/" + (database != null ? database.getInternalName() : "");
        dataSource.setJdbcUrl(url);
        dataSource.setUser(user.getUsername());
        dataSource.setPassword(user.getDatabasePassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        log.trace("created pooled data source {}", dataSource);
        return dataSource;
    }

}
