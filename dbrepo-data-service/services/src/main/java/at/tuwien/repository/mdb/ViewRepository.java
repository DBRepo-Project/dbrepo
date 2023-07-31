package at.tuwien.repository.mdb;

import at.tuwien.entities.database.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

    Optional<View> findByInternalName(String internalName);

}
