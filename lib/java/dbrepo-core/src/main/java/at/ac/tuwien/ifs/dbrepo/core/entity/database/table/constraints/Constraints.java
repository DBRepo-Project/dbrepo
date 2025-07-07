package at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.primaryKey.PrimaryKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import lombok.*;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.LinkedList;
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
    private List<Unique> uniques = new LinkedList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "table")
    private List<ForeignKey> foreignKeys = new LinkedList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "mdb_constraints_checks", joinColumns = {
            @JoinColumn(name = "tid"),
    })
    private Set<String> checks = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "table")
    private List<PrimaryKey> primaryKey = new LinkedList<>();
}
