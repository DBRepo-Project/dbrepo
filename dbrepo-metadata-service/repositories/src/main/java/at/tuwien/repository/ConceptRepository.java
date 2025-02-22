package at.tuwien.repository;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConceptRepository extends JpaRepository<TableColumnConcept, UUID> {

    Optional<TableColumnConcept> findByUri(String uri);

}
