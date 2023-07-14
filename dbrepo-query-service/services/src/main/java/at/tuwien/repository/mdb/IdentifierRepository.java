package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.Identifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

    Optional<Identifier> findDatabaseIdentifier(Long databaseId);

    Optional<Identifier> findSubsetIdentifier(Long databaseId, Long queryId);

    List<Identifier> findAllDatabaseIdentifiers();

    List<Identifier> findAllSubsetIdentifiers();

}
