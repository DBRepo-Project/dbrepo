package at.tuwien.repository.jpa;

import at.tuwien.entities.auth.Realm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RealmRepository extends JpaRepository<Realm, String> {

    Optional<Realm> findByName(String name);

}
