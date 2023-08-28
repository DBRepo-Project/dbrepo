package at.tuwien.repository.mdb;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableColumnConceptRepository extends JpaRepository<TableColumnConcept, String> {

}
