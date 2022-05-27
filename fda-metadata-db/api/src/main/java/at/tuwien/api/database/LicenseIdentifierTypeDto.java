package at.tuwien.api.database;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum LicenseIdentifierTypeDto {
    MIT("MIT"),
    GNU_GPL3("GPL-3.0-only"),
    BSD3("BSD-3-Clause"),
    BSD4("BSD-4-Clause"),
    APACHE2("Apache-2.0"),
    CC_01("CC0-1.0"),
    CC_BY4("CC-BY-4.0");

    private final String name;

    LicenseIdentifierTypeDto(String name) {
        this.name = name;
    }
}
