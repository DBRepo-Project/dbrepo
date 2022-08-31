package at.tuwien.mapper;

import at.tuwien.api.database.LicenseDto;
import at.tuwien.entities.database.License;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LicenseMapper {

    LicenseDto licenseToLicenseDto(License data);

}
