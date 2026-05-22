package at.ac.tuwien.ifs.dbrepo.metadata;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ForeignKeyRepository extends JpaRepository<ForeignKey, UUID> {

    @Modifying
    @Query("""
            delete from ForeignKeyReference r
            where r.column.id in (
                select c.id
                from TableColumn c
                where c.table.id = :tableId
            )
            or r.referencedColumn.id in (
                select c.id
                from TableColumn c
                where c.table.id = :tableId
            )
            """)
    int deleteReferencesByTableId(@Param("tableId") UUID tableId);

    @Modifying
    @Query("""
            delete from ForeignKey foreignKey
            where foreignKey.table.id = :tableId
            or foreignKey.referencedTable.id = :tableId
            """)
    int deleteByTableId(@Param("tableId") UUID tableId);

}
