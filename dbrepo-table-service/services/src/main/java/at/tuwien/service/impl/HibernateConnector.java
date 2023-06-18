package at.tuwien.service.impl;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.Database;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public abstract class HibernateConnector {

    protected static ComboPooledDataSource getPrivilegedDataSource(ContainerImage image, Container container) {
        return getPrivilegedDataSource(image, container, null);
    }

    protected static ComboPooledDataSource getPrivilegedDataSource(ContainerImage image, Container container, Database database) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        final String url = "jdbc:" + image.getJdbcMethod() + "://" + container.getHost() + ":" + container.getPort() + "/" + (database != null ? database.getInternalName() : "");
        dataSource.setJdbcUrl(url);
        dataSource.setUser(container.getPrivilegedUsername());
        dataSource.setPassword(container.getPrivilegedPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        log.trace("mapped data source {}", dataSource);
        return dataSource;
    }

}
