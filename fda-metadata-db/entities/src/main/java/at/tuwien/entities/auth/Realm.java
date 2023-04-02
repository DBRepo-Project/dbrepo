package at.tuwien.entities.auth;

import lombok.*;

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
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sslRequired;

}
