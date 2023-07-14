package at.tuwien.repository.mdb;

import at.tuwien.entities.database.table.columns.TableColumnUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<TableColumnUnit, Long> {

    /**
     * Finds a semantic unit by URI.
     *
     * @param uri The URI.
     * @return Optional semantic unit that matches this filter.
     */
    Optional<TableColumnUnit> findByUri(String uri);

}
