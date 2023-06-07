package at.tuwien.repository.mdb;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.TableKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableRepository extends JpaRepository<Table, TableKey> {

}
