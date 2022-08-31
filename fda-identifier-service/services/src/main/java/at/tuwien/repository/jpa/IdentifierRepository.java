package at.tuwien.repository.jpa;

import at.tuwien.entities.identifier.Identifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

    List<Identifier> findByDbid(Long databaseId);

    Optional<Identifier> findByDbidAndQid(Long databaseId, Long queryId);

}
