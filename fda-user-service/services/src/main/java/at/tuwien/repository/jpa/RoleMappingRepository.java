package at.tuwien.repository.jpa;

import at.tuwien.entities.user.RoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleMappingRepository extends JpaRepository<RoleMapping, String> {
}
