package at.tuwien.repository.mdb;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<Table, Long> {

    @Query(value = "select t from Table t where t.database.id = :databaseId and t.id = :tableId")
    Optional<Table> find(@Param("databaseId") Long databaseId, @Param("tableId") Long tableId);

    Optional<Table> findByDatabaseIdAndId(Long databaseId, Long tableId);

    /**
     * Finds all tables by database.
     *
     * @param database The database.
     * @return List of tables.
     */
    List<Table> findByDatabaseOrderByCreatedDesc(Database database);

    List<Table> findByInternalName(String internalName);

    /**
     * Finds a table with given database and internal name.
     *
     * @param database     The database.
     * @param internalName The internal name.
     * @return Optional table that matches this filter.
     */
    Optional<Table> findByDatabaseAndInternalName(Database database, String internalName);

    /**
     * Finds a table with database id and internal name.
     *
     * @param tdbid        The database id.
     * @param internalName The internal name.
     * @return Optional table that matches this filter.
     */
    Optional<Table> findByTdbidAndInternalName(Long tdbid, String internalName);

    /**
     * Finds a table with database id and table id.
     *
     * @param database The database id.
     * @param tableId  The table id.
     * @return Optional table that matches this filter.
     */
    Optional<Table> findByDatabaseAndId(Database database, Long tableId);

}
