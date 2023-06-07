package at.tuwien.repository.mdb;

import at.tuwien.entities.container.image.ContainerImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<ContainerImage, Long> {

}
