package at.ac.tuwien.ifs.dbrepo.core.entity.database;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
public class DatabaseAccessKey implements Serializable {

    private String username;

    private UUID hdbid;
}
