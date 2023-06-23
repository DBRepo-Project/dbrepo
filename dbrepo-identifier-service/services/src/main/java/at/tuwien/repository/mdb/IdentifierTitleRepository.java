package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.IdentifierTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentifierTitleRepository extends JpaRepository<IdentifierTitle, Long> {

}
