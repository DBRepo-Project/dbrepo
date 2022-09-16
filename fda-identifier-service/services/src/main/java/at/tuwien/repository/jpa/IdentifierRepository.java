package at.tuwien.repository.jpa;

import at.tuwien.entities.identifier.Identifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

    List<Identifier> findByDatabaseId(Long databaseId);

    Optional<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId);

}
