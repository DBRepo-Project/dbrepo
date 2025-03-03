package at.tuwien.repository;

import at.tuwien.entities.container.Container;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContainerRepository extends JpaRepository<Container, UUID> {

    Optional<Container> findByInternalName(String internalName);

    List<Container> findByOrderByCreatedDesc(Pageable pageable);

}
