package at.tuwien.entities.user;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
public class RealmDefaultGroupKey implements Serializable {

    private UUID groupId;

    private UUID realmId;

}
