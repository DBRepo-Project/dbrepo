package at.tuwien.entities.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "user_attribute")
public class UserAttribute {

    @Id
    @JsonIgnore
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "attribute-uuid")
    @GenericGenerator(name = "attribute-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @JsonIgnore
    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "VALUE")
    private String value;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "USER_ID", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User user;

}
