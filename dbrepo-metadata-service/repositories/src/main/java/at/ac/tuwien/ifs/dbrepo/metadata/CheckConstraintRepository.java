package at.ac.tuwien.ifs.dbrepo.metadata;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.check.CheckConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CheckConstraintRepository extends JpaRepository<CheckConstraint, UUID> {
}
