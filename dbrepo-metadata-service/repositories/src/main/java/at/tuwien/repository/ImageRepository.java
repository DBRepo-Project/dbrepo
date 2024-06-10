package at.tuwien.repository;

import at.tuwien.entities.container.image.ContainerImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<ContainerImage, Long> {

    List<ContainerImage> findAll();

    Optional<ContainerImage> findByNameAndVersion(String name, String version);

}
