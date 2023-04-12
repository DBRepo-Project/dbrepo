package at.tuwien.entities.user;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class RoleMappingKey implements Serializable {

    private String userId;

    private String roleId;

}
