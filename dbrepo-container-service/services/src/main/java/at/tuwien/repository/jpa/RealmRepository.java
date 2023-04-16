package at.tuwien.repository.jpa;

import at.tuwien.entities.user.Realm;
import at.tuwien.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RealmRepository extends JpaRepository<Realm, String> {

}
