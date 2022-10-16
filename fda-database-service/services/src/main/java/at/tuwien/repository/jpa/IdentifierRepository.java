package at.tuwien.repository.jpa;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

    List<Identifier> findByContainerId(Long containerId);

    Optional<Identifier> findByContainerIdAndDatabaseIdAndType(Long containerId, Long databaseId, IdentifierType type);

}
