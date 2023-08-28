package at.tuwien.repository.mdb;

import at.tuwien.entities.user.UserAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAttributeRepository extends JpaRepository<UserAttribute, UUID> {

    Optional<UserAttribute> findByUserIdAndName(UUID userId, String name);

}
