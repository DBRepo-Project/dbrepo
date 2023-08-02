package at.tuwien.repository.mdb;

import at.tuwien.entities.database.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, String> {

    Optional<License> findByIdentifier(String identifier);

}
