package at.tuwien.entities.database.table.constraints;

import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import lombok.*;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Constraints {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE, mappedBy = "table")
    @OrderColumn(name = "position")
    private List<Unique> uniques;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE, mappedBy = "table")
    @OrderColumn(name = "position")
    private List<ForeignKey> foreignKeys;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mdb_constraints_checks", joinColumns = {
            @JoinColumn(name = "tid", insertable = false, updatable = false),
            @JoinColumn(name = "tdbid", insertable = false, updatable = false)
    })
    private Set<String> checks;
}
