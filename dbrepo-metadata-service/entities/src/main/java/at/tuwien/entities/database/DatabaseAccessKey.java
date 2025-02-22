package at.tuwien.entities.database;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
public class DatabaseAccessKey implements Serializable {

    private UUID huserid;

    private UUID hdbid;
}
