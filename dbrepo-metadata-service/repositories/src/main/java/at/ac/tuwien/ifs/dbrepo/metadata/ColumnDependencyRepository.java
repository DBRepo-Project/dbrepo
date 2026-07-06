package at.ac.tuwien.ifs.dbrepo.metadata;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ColumnDependencyRepository extends JpaRepository<TableColumn, UUID> {

    @Modifying
    @Query(value = """
            delete from mdb_columns_concepts
            where cID in (
                select id
                from mdb_columns
                where tID = :tableId
            )
            """, nativeQuery = true)
    int deleteConceptsByTableId(@Param("tableId") String tableId);

    @Modifying
    @Query(value = """
            delete from mdb_columns_units
            where cID in (
                select id
                from mdb_columns
                where tID = :tableId
            )
            """, nativeQuery = true)
    int deleteUnitsByTableId(@Param("tableId") String tableId);

    @Modifying
    @Query(value = """
            delete from mdb_columns_enums
            where column_id in (
                select id
                from mdb_columns
                where tID = :tableId
            )
            """, nativeQuery = true)
    int deleteEnumsByTableId(@Param("tableId") String tableId);

    @Modifying
    @Query(value = """
            delete from mdb_columns_sets
            where column_id in (
                select id
                from mdb_columns
                where tID = :tableId
            )
            """, nativeQuery = true)
    int deleteSetsByTableId(@Param("tableId") String tableId);

}
