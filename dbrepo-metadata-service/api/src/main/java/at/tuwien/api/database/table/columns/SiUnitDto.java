package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum SiUnitDto {

    @JsonProperty("second")
    SECOND("second"),

    @JsonProperty("meter")
    METER("meter"),

    @JsonProperty("kilogram")
    KILOGRAM("kilogram"),

    @JsonProperty("ampere")
    AMPERE("ampere"),

    @JsonProperty("kelvin")
    KELVIN("kelvin"),

    @JsonProperty("mole")
    MOLE("mole"),

    @JsonProperty("candela")
    CANDELA("candela");

    private String name;

    SiUnitDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
