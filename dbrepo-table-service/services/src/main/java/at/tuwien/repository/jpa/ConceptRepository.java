package at.tuwien.repository.jpa;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptRepository extends JpaRepository<TableColumnConcept, String> {

    Optional<TableColumnConcept> findById(String id);

}
