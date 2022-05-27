package at.tuwien.repository.jpa;

import at.tuwien.entities.database.License;
import at.tuwien.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, String> {

}
