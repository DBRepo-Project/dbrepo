package at.tuwien.repository;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImageDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByInternalName(String internalName);

    List<Container> findByOrderByCreatedDesc(Pageable pageable);

}
