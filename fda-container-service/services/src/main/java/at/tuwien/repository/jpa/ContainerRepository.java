package at.tuwien.repository.jpa;

import at.tuwien.entities.container.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    @Query("select c from Container c where c.isPublic = true")
    List<Container> findAllPublic();

    @Query("select c from Container c where c.isPublic = true or c.creator.username = :username")
    List<Container> findAllAndByCreator(@Param("username") String username);

}
