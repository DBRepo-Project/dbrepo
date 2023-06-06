package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.CreatorKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, CreatorKey> {

}
