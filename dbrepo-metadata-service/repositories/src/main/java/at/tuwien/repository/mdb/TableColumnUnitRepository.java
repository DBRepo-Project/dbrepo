package at.tuwien.repository.mdb;

import at.tuwien.entities.database.table.columns.TableColumnUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableColumnUnitRepository extends JpaRepository<TableColumnUnit, String> {

}
