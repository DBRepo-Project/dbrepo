package at.tuwien.repository.jpa;

import at.tuwien.entities.user.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, String> {

}
