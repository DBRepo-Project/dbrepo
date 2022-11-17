package at.tuwien.repository.jpa;

import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerImageEnvironmentItemRepository extends JpaRepository<ContainerImageEnvironmentItem, Long> {

}
