package at.tuwien.repository.mdb;

import at.tuwien.entities.user.RoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoleMappingRepository extends JpaRepository<RoleMapping, UUID> {
}
