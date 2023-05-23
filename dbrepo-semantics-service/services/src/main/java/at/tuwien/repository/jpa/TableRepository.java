package at.tuwien.repository.jpa;

import at.tuwien.entities.database.table.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<Table, Long> {

    Optional<Table> findByDatabaseIdAndId(Long databaseId, Long tableId);

}
