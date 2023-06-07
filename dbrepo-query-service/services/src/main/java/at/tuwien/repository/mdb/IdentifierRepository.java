package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.Identifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

    Optional<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId);

}
