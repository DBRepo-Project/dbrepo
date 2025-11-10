package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum AccessType {
    READ,
    WRITE_OWN,
    WRITE_ALL
}
