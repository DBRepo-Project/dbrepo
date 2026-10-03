package at.ac.tuwien.ifs.dbrepo.metadata;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReplicationNotificationOutboxRepository extends JpaRepository<ReplicationNotificationOutbox, UUID> {

    @Query("select e from ReplicationNotificationOutbox e where " +
            "(e.status = :status and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)) or " +
            "(e.status = :failed and e.nextAttemptAt <= :now) order by e.nextAttemptAt asc, e.created asc")
    List<ReplicationNotificationOutbox> findDue(@Param("status") ReplicationNotificationStatus status,
                                                @Param("failed") ReplicationNotificationStatus failed,
                                                @Param("now") Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ReplicationNotificationOutbox e where e.id = :id")
    Optional<ReplicationNotificationOutbox> findForUpdate(@Param("id") UUID id);
}
