package at.tuwien.querystore;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@javax.persistence.Table(name = "qs_views")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
public class View implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "view-sequence")
    @GenericGenerator(
            name = "view-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "qs_views_seq")
    )
    private Long id;

    @Column(nullable = false)
    private Long vdbid;

    @Column(nullable = false)
    private Long createdBy;

    @Column(name = "vname", nullable = false)
    private String name;

    @Column(name = "public", nullable = false)
    private Boolean isPublic;

    @Column(name = "initialview", nullable = false)
    private Boolean isInitialView;

    @Column(nullable = false)
    private String query;

    @Column
    private Instant deleted;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column(name = "last_modified")
    @LastModifiedDate
    private Instant lastModified;

}
