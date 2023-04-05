package at.tuwien.entities.auth;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "realm")
public class Realm {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "realm-uuid")
    @GenericGenerator(name = "realm-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private String name;

}
