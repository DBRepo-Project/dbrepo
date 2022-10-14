package at.tuwien.repositories;

import at.tuwien.entities.user.TimeSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimeSecretRepository extends JpaRepository<TimeSecret, Long> {

    Optional<TimeSecret> findByToken(String token);

}
