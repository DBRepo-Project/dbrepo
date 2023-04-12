package at.tuwien.entities.database;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class DatabaseAccessKey implements Serializable {

    private String huserid;

    private Long hdbid;
}
