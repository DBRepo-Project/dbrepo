package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.identifier.RelatedIdentifierKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelatedIdentifierRepository extends JpaRepository<RelatedIdentifier, RelatedIdentifierKey> {

}
