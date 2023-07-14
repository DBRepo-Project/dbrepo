package at.tuwien.repository.mdb;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.DatabaseAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseAccessRepository extends JpaRepository<DatabaseAccess, DatabaseAccessKey> {

}
