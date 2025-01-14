package at.tuwien.repository;

import at.tuwien.entities.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    List<Database> findAllDesc();

    List<Database> findAllPublicOrSchemaPublicDesc();

    List<Database> findAllAtLestReadAccessDesc(UUID id);

    List<Database> findAllPublicOrSchemaPublicOrReadAccessDesc(UUID id);

    List<Database> findAllPublicOrSchemaPublicOrReadAccessByInternalNameDesc(UUID id, String internalName);

    List<Database> findAllPublicOrSchemaPublicByInternalNameDesc(String internalName);

}
