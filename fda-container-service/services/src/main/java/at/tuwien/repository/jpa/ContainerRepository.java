package at.tuwien.repository.jpa;

import at.tuwien.entities.container.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByHash(String hash);

    Optional<Container> findByInternalName(String internalName);

}
