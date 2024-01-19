package at.tuwien.entities.database.table.constraints;

import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import lombok.*;

import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Constraints {

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "table")
    @OrderColumn(name = "position")
    private List<Unique> uniques;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "table")
    @OrderColumn(name = "position")
    private List<ForeignKey> foreignKeys;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "mdb_constraints_checks", joinColumns = {
            @JoinColumn(name = "tid"),
    })
    private Set<String> checks;
}
