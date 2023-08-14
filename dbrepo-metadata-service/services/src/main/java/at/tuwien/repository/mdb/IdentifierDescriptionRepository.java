package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.IdentifierDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentifierDescriptionRepository extends JpaRepository<IdentifierDescription, Long> {
}
