package at.tuwien.repository.jpa;

import at.tuwien.entities.database.table.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<Table, Long> {

    @Query(value = "select t from Table t where t.database.container.id = :containerId and t.database.id = :databaseId and t.id = :tableId")
    Optional<Table> find(@Param("containerId") Long containerId, @Param("databaseId") Long databaseId,
                         @Param("tableId") Long tableId);

}
