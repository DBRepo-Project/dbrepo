package at.tuwien.repository.jpa;

import at.tuwien.entities.user.UserAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAttributeRepository extends JpaRepository<UserAttribute, String> {

    List<UserAttribute> findByUser(String userId);

    Optional<UserAttribute> findByUserIdAndName(String userId, String name);

}
