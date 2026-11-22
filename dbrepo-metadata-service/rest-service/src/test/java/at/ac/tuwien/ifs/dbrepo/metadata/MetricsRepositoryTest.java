package at.ac.tuwien.ifs.dbrepo.metadata;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierType;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MetricsRepositoryTest extends BaseTest {

    @MockitoBean private DatabaseCacheRepository databaseCacheRepository;

    @Autowired private DatabaseRepository databases;
    @Autowired private ViewRepository views;
    @Autowired private TableRepository tables;
    @Autowired private IdentifierRepository identifiers;
    @Autowired private LicenseRepository licenses;
    @Autowired private ContainerRepository containers;
    @Autowired private EntityManager entityManager;

    @Test
    void catalogGaugesReadFiveScalarsWithoutLoadingEntities() {
        assertEquals(0, tables.sumDataLength());
        assertEquals(0, identifiers.countByType(IdentifierType.SUBSET));

        TABLE_1.setDataLength(15L);
        TABLE_2.setDataLength(null);
        TABLE_3.setDataLength(27L);
        TABLE_4.setDataLength(null);
        licenses.save(LICENSE_1);
        containers.save(CONTAINER_1);
        databases.saveAndFlush(DATABASE_1);
        int databaseCount = databases.findAll().size();
        int viewCount = views.findAll().size();
        var tableRows = tables.findAll();
        long volume = tableRows.stream().map(Table::getDataLength).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        int subsetCount = identifiers.findAllSubsetIdentifiers().size();
        entityManager.clear();

        var stats = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        assertEquals(databaseCount, databases.count());
        assertEquals(viewCount, views.count());
        assertEquals(tableRows.size(), tables.count());
        assertEquals(subsetCount, identifiers.countByType(IdentifierType.SUBSET));
        assertEquals(volume, tables.sumDataLength());
        assertEquals(5, stats.getPrepareStatementCount());
        assertEquals(0, stats.getEntityLoadCount());

        entityManager.createQuery("update Table t set t.dataLength = null").executeUpdate();
        assertEquals(0, tables.sumDataLength());
    }
}
