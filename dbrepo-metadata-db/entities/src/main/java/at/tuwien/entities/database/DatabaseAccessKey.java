package at.tuwien.entities.database;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class DatabaseAccessKey implements Serializable {

    private Long huserid;

    private Long hdbid;
}
