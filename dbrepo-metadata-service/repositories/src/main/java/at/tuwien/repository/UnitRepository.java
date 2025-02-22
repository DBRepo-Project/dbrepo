package at.tuwien.repository;

import at.tuwien.entities.database.table.columns.TableColumnUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<TableColumnUnit, UUID> {

    Optional<TableColumnUnit> findByUri(String uri);

}
