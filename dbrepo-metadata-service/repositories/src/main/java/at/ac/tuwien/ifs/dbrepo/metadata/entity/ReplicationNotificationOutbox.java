package at.ac.tuwien.ifs.dbrepo.metadata.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_replication_notification_outbox", indexes = {
        @Index(name = "idx_mdb_replication_notification_outbox_due", columnList = "status,next_attempt_at"),
        @Index(name = "idx_mdb_replication_notification_outbox_aggregate", columnList = "aggregate_id")
})
public class ReplicationNotificationOutbox implements Serializable {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 64)
    private ReplicationNotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReplicationNotificationStatus status;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(nullable = false)
    private String path;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "aggregate_id", columnDefinition = "VARCHAR(36)")
    private UUID aggregateId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @LastModifiedDate
    @Column(name = "last_modified", columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant nextAttemptAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = ReplicationNotificationStatus.PENDING;
        }
        if (this.created == null) {
            this.created = Instant.now();
        }
        if (this.nextAttemptAt == null) {
            this.nextAttemptAt = Instant.now();
        }
    }
}
