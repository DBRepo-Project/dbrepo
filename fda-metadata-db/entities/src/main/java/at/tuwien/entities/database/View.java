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
@IdClass(ViewKey.class)
@Where(clause = "deleted is null")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "update mdb_view set deleted = NOW() where id = ?")
@javax.persistence.Table(name = "mdb_view")
public class View {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "view-sequence")
    @GenericGenerator(
            name = "view-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_view_seq")
    )
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long vdbid;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "UserID")
    })
    private User creator;

    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "vdbid", insertable = false, updatable = false)
    private Database database;

    @Column(name = "vname")
    private String name;

    @Column(name = "public")
    private Boolean isPublic;

    @Column(name = "initialview")
    private Boolean isInitialView;

    @Column(nullable = false)
    private String query;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

    @Column
    private Instant deleted;

}
