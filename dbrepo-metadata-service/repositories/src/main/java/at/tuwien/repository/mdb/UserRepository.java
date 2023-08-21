package at.tuwien.repository.mdb;

import at.tuwien.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by username.
     *
     * @param username The username.
     * @return Optional user that matches this filter.
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

}
