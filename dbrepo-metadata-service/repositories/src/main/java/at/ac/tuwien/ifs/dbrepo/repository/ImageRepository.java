package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<ContainerImage, UUID> {

    Optional<ContainerImage> findByNameAndVersion(String name, String version);

    Optional<ContainerImage> findByIsDefault(Boolean isDefault);

}
