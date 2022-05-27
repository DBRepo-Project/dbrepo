package at.tuwien.entities.database;

import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@javax.persistence.Table(name = "mdb_subjects")
public class Subject {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "subjects-sequence")
    @GenericGenerator(
            name = "subjects-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_subjects_seq")
    )
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "UserID")
    })
    private User creator;

    @Column(nullable = false)
    private String name;

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "mdb_databases_subjects", joinColumns = {
            @JoinColumn(name = "sid", referencedColumnName = "id", insertable = false, updatable = false)
    }, inverseJoinColumns = {
            @JoinColumn(name = "dbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<Database> database;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}
