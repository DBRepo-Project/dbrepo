package at.tuwien.repository.mdb;

import at.tuwien.entities.semantics.Ontology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OntologyRepository extends JpaRepository<Ontology, Long> {

    List<Ontology> findAllProcessable();

}
