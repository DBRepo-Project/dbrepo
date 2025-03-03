package at.tuwien.repository;

import at.tuwien.entities.database.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * TO BE USED READONLY
 */
@Repository
public interface ViewRepository extends JpaRepository<View, UUID> {

}
