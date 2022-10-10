package at.tuwien.entities.database;

import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(QueryKey.class)
@Where(clause = "deleted is null")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "update mdb_queries set deleted = NOW() where id = ?")
@javax.persistence.Table(name = "mdb_queries")
public class Query {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "queries-sequence")
    @GenericGenerator(
            name = "queries-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_queries_seq")
    )
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long cid;

    @Id
    @EqualsAndHashCode.Include
    private Long dbid;

    @Column
    private Instant execution;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String queryNormalized;

    @Column(nullable = false)
    private String queryHash;

    @Column
    private String resultHash;

    @Column
    private Long resultNumber;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "Creator", referencedColumnName = "UserID")
    })
    private User creator;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

    @Column
    private Instant deleted;

}
