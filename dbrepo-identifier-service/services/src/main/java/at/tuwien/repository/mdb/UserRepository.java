
package at.tuwien.repository.mdb;

import at.tuwien.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds an user by given username.
     *
     * @param username The username.
     * @return Non-empty optional if this user exists, empty optional otherwise.
     */
    Optional<User> findByUsername(String username);

}
