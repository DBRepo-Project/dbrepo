package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

    List<Identifier> findByDatabaseId(Long databaseId);

    List<Identifier> findByQueryId(Long queryId);

    Optional<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId);

    Boolean existsByDatabaseIdAndType(Long databaseId, IdentifierType type);

    Boolean existsByDatabaseIdAndQueryIdAndType(Long databaseId, Long queryId, IdentifierType type);

}
