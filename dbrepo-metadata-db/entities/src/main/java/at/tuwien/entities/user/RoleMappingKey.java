package at.tuwien.entities.user;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
public class RoleMappingKey implements Serializable {

    private UUID userId;

    private UUID roleId;

}
