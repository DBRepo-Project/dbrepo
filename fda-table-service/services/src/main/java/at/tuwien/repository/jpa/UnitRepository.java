package at.tuwien.repository.jpa;

import at.tuwien.entities.database.table.columns.units.ColumnUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepository extends JpaRepository<ColumnUnit, String> {

}
