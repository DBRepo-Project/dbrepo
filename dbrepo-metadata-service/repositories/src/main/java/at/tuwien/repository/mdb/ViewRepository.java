package at.tuwien.repository.mdb;

import at.tuwien.entities.database.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

    List<View> findAllPublicByDatabaseId(Long databaseId);

    List<View> findAllPublicOrMineByDatabaseId(Long databaseId, UUID userId);

    Optional<View> findPublicByDatabaseIdAndId(Long databaseId, Long id);

    Optional<View> findPublicOrMineByDatabaseIdAndId(Long databaseId, Long id, UUID userId);

}

