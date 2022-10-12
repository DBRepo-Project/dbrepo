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

    @Query("select v from View v where v.database.id = :databaseId")
    List<View> findAllByDatabaseId(@Param("databaseId") Long databaseId);

    @Query("select v from View v where v.database.id = :databaseId and v.isPublic is true")
    List<View> findAllPublicByDatabaseId(@Param("databaseId") Long databaseId);

    @Query("select v from View v where v.database.id = :databaseId and (v.isPublic is true or v.creator.username = :username)")
    List<View> findAllPublicOrMineByDatabaseId(@Param("databaseId") Long databaseId, @Param("username") String username);

    @Query("select v from View v where v.database.id = :databaseId and v.id = :id")
    Optional<View> findByDatabaseIdAndId(@Param("databaseId") Long databaseId, @Param("id") Long id);

    @Query("select v from View v where v.database.id = :databaseId and v.id = :id and v.isPublic is true")
    Optional<View> findPublicByDatabaseIdAndId(@Param("databaseId") Long databaseId, @Param("id") Long id);

    @Query("select v from View v where v.database.id = :databaseId and v.id = :id and (v.isPublic is true or v.creator.username = :username)")
    Optional<View> findPublicOrMineByDatabaseIdAndId(@Param("databaseId") Long databaseId, @Param("id") Long id, @Param("username") String username);

}

