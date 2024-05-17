package at.tuwien.repository;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptRepository extends JpaRepository<TableColumnConcept, Long> {

    Optional<TableColumnConcept> findByUri(String uri);

}
