package at.tuwien.entities.user;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "realm")
@NamedQueries({
        @NamedQuery(name = "Realm.findAll", query = "select r from Realm r where r.name = 'dbrepo'")
})
public class Realm {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private String name;

}
