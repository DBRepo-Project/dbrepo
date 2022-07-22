package at.tuwien.service.impl;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.Database;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


}
