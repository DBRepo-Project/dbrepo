package at.tuwien.repository.mdb;

import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.entities.container.image.ContainerImageDateKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageDateRepository extends JpaRepository<ContainerImageDate, ContainerImageDateKey> {

}
