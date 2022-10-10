package at.tuwien.entities.database;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class QueryKey implements Serializable {

    private Long id;

    private Long cid;

    private Long dbid;

}
