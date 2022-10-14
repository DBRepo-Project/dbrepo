package at.tuwien.entities.database;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class ViewKey implements Serializable {

    private Long id;

    private Long vcid;

    private Long vdbid;
}
