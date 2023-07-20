package at.tuwien.repository.mdb;

import at.tuwien.entities.database.View;
import at.tuwien.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

}
