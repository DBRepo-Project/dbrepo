package at.tuwien.entities.identifier;

import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@IdClass(CreatorKey.class)
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_creators")
public class Creator {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "creators-sequence")
    @GenericGenerator(name = "creators-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long pid;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Column
    private String affiliation;

    @Column
    private String orcid;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", referencedColumnName = "id", insertable = false, updatable = false)
    private Identifier identifier;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @Field(name = "created_by")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false)
    })
    private User creator;

    @LastModifiedDate
    @Field(name = "last_modified")
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
