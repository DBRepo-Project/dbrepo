package at.tuwien.entities.user;

import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.*;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "realm")
@NamedQueries({
        @NamedQuery(name = "Realm.findAll", query = "select r from Realm r where r.name = 'dbrepo'")
})
public class Realm {

    @Id
    @EqualsAndHashCode.Include
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private String name;

}
