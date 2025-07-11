package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, UUID> {

    List<Database> findAllDesc();

    List<Database> findAllPublicOrSchemaPublicDesc();

    List<Database> findAllAtLestReadAccessDesc(String username);

    List<Database> findAllPublicOrSchemaPublicOrReadAccessDesc(String username);

    List<Database> findAllPublicOrSchemaPublicOrReadAccessByInternalNameDesc(String username, String internalName);

    List<Database> findAllPublicOrSchemaPublicByInternalNameDesc(String internalName);

    List<Database> findAllByInternalNameDesc(String internalName);

}
