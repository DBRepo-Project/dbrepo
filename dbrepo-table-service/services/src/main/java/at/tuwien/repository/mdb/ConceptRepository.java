package at.tuwien.repository.mdb;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptRepository extends JpaRepository<TableColumnConcept, Long> {

    /**
     * Retrieve a column concept by URI.
     *
     * @param uri The URI.
     * @return Optional table column concept.
     */
    Optional<TableColumnConcept> findByUri(String uri);

}
