package at.tuwien.entities.database.table.columns.concepts;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class ColumnConceptKey implements Serializable {

    private Long cid;

    private Long cdbid;

    private Long tid;

}
