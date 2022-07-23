package at.tuwien.service.impl;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.Database;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.NativeQuery;
import org.hibernate.service.ServiceRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public abstract class HibernateConnector {

    protected static Session getCurrentSession(ContainerImage image, Container container, Database database) {
        final String url = "jdbc:" + image.getJdbcMethod() + "://" + container.getInternalName() + "/" + database.getInternalName();
        final String username = image.getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);
        final String password = image.getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);

        final Configuration config = new Configuration();
        config.configure("mariadb_hibernate.cfg.xml");
        config.setProperty("hibernate.connection.url", url);
        config.setProperty("hibernate.connection.username", username);
        config.setProperty("hibernate.connection.password", password);
        config.setProperty("hibernate.connection.driver_class", image.getDriverClass());
        config.setProperty("hibernate.dialect", image.getDialect());
        final SessionFactory sessionFactory = config.buildSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        if (!session.isOpen()) {
            log.warn("Session is closed, opening...");
            session = sessionFactory.openSession();
        }
        return session;
    }

    protected static Long activeConnection(Session session) {
        final NativeQuery<?> nativeQuery = session.createSQLQuery("SHOW STATUS LIKE 'threads_connected'");
        final List<?> result;
        try {
            result = nativeQuery.getResultList();
        } catch (PersistenceException e) {
            log.error("Failed to collect number of used connections");
            /* ignore */
            return null;
        }
        final Object[] row = (Object[]) result.get(0);
        log.debug("current number of connections: {}", Long.parseLong(String.valueOf(row[1])));
        return Long.parseLong(String.valueOf(row[1]));
    }

    /**
     * Checks if the word is in the reserved word csv (i.e. an SQL keyword), solves issue 106
     *
     * @param word The word
     * @return True if it is reserved word
     */
    public static Boolean isReserved(String word) throws IOException {
        final InputStream stream = new ClassPathResource("mariadb/reserved.csv").getInputStream();
        final List<String> reserved = IOUtils.readLines(stream, "UTF-8");
        return reserved.contains(word.toUpperCase());
    }


}
