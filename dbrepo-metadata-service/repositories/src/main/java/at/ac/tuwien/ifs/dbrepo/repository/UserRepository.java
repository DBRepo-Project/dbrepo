package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    List<User> findAllInternal();

    boolean existsByUsername(String username);

}

