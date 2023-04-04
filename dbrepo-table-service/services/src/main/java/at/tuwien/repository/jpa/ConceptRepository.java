package at.tuwien.repository.jpa;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptRepository extends JpaRepository<TableColumnConcept, String> {

}
