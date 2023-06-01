package at.tuwien.repository.jpa;

import at.tuwien.entities.database.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

    List<View> findAllPublicByDatabaseId(Long databaseId);

    List<View> findAllPublicOrMineByDatabaseId(Long databaseId, String username);

    Optional<View> findPublicByDatabaseIdAndId(Long databaseId, Long id);

    Optional<View> findPublicOrMineByDatabaseIdAndId(Long databaseId, Long id, String username);

}

