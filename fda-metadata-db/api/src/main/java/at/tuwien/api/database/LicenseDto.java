package at.tuwien.api.database;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum LicenseDto {
    MIT2,
    GNU_GPL3,
    BSD2,
    BSD3,
    APACHE2,
    CC_0,
    CC_BY
}
