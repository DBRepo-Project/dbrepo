package at.tuwien.entities.database;

import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
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
@EntityListeners(AuditingEntityListener.class)
@javax.persistence.Table(name = "mdb_view")
public class View {

    @Id
    @EqualsAndHashCode.Include
    @GenericGenerator(name = "native", strategy = "native")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long vcid;

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

    @Column(name = "vname", nullable = false)
    private String name;

    @Column(nullable = false)
    private String internalName;

    @Column(name = "public", nullable = false)
    private Boolean isPublic;

    @Column(name = "initialview", nullable = false)
    private Boolean isInitialView;

    @Column(nullable = false, columnDefinition = "TEXT")
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
