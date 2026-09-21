package at.ac.tuwien.ifs.dbrepo.metadata;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationIdentityMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReplicationIdentityMappingRepository extends JpaRepository<ReplicationIdentityMapping, UUID> {

    Optional<ReplicationIdentityMapping> findByOriginSiteAndOriginIssuerAndOriginSubject(
            String originSite, String originIssuer, String originSubject);

}
