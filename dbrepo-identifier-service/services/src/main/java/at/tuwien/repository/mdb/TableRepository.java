package at.tuwien.repository.mdb;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.TableKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<Table, TableKey> {

    List<Table> findByDatabase(Database database);

    Optional<Table> findByDatabaseAndId(Database database, Long tableId);

    Optional<Table> findByInternalName(String internalName);

}
