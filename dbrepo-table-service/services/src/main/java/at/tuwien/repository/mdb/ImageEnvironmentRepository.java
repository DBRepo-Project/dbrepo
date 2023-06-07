package at.tuwien.repository.mdb;

import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageEnvironmentRepository extends JpaRepository<ContainerImageEnvironmentItem, ContainerImageEnvironmentItemKey> {

}
