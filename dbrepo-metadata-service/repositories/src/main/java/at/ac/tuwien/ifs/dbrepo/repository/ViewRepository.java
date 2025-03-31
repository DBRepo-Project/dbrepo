package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * TO BE USED READONLY
 */
@Repository
public interface ViewRepository extends JpaRepository<View, UUID> {

}
