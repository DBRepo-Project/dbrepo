package at.ac.tuwien.ifs.dbrepo.metadata;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * TO BE USED READONLY
 */
@Repository
public interface TableRepository extends JpaRepository<Table, UUID> {

    @Query("select coalesce(sum(t.dataLength), 0) from Table t")
    long sumDataLength();

}
