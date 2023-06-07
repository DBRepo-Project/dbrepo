package at.tuwien.repository.mdb;

import at.tuwien.entities.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    @Query(value = "select d from Database d where d.container.id = :containerId and d.id = :databaseId")
    Optional<Database> findByContainerAndDatabaseId(@Param("containerId") Long containerId,
                                                    @Param("databaseId") Long databaseId);
    
}
