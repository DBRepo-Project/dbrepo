package at.tuwien.repository.mdb;

import at.tuwien.entities.user.Realm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RealmRepository extends JpaRepository<Realm, UUID>  {
}
