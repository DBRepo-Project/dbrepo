package at.tuwien.repository;

import at.tuwien.entities.semantics.Ontology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OntologyRepository extends JpaRepository<Ontology, UUID> {

    List<Ontology> findAllProcessable();

    Optional<Ontology> findByUriPattern(String uriPattern);

}
