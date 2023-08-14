package at.tuwien.mapper;

import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.api.orcid.activities.employments.affiliation.OrcidAffiliationGroupDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.OrcidEmploymentSummaryDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedSourceTypeDto;
import at.tuwien.api.ror.RorDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.api.user.external.ExternalResultType;
import at.tuwien.api.user.external.affiliation.ExternalAffiliationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", imports = {ExternalResultType.class})
public interface ExternalMapper {


    @Mappings({
            @Mapping(target = "givenNames", source = "person.name.givenNames.value"),
            @Mapping(target = "familyName", source = "person.name.familyName.value"),
            @Mapping(target = "type", expression = "java(ExternalResultType.PERSONAL)"),
            @Mapping(target = "affiliations", source = "activitiesSummary.employments.affiliationGroup"),
    })
    ExternalMetadataDto orcidDtoToExternalMetadataDto(OrcidDto data);

    @Mappings({
            @Mapping(target = "organizationName", source = "employmentSummary.organization.name"),
            @Mapping(target = "ringgoldId", expression = "java(disambiguatedOrganizationToRinggoldId(data.getEmploymentSummary().getOrganization().getDisambiguatedOrganization()))"),
    })
    ExternalAffiliationDto orcidEmploymentSummaryDtoToExternalAffiliationDto(OrcidEmploymentSummaryDto data);

    default ExternalAffiliationDto orcidAffiliationGroupDtoToExternalAffiliationDto(OrcidAffiliationGroupDto data) {
        if (data == null || data.getSummaries() == null || data.getSummaries().length == 0) {
            return null;
        }
        return ExternalAffiliationDto.builder()
                .organizationName(data.getSummaries()[0].getEmploymentSummary().getOrganization().getName())
                .build();
    }

    default Long disambiguatedOrganizationToRinggoldId(OrcidDisambiguatedDto data) {
        if (data.getSource().equals(OrcidDisambiguatedSourceTypeDto.RINGGOLD)) {
            return Long.parseLong(data.getIdentifier());
        }
        return null;
    }

    default ExternalMetadataDto rorDtoToExternalMetadataDto(RorDto data) {
        return ExternalMetadataDto.builder()
                .affiliations(new ExternalAffiliationDto[]{
                        ExternalAffiliationDto.builder()
                                .organizationName(data.getName())
                                .build()})
                .type(ExternalResultType.ORGANIZATIONAL)
                .build();
    }

    default ExternalMetadataDto crossrefDtoToExternalMetadataDto(CrossrefDto data) {
        return ExternalMetadataDto.builder()
                .affiliations(new ExternalAffiliationDto[]{
                        ExternalAffiliationDto.builder()
                                .crossrefFunderId(data.getId())
                                .organizationName(data.getPrefLabel().getLabel().getLiteralForm().getContent())
                                .build()})
                .type(ExternalResultType.ORGANIZATIONAL)
                .build();
    }

    @Mappings({
            @Mapping(target = "organizationName", source = "name"),
    })
    ExternalAffiliationDto rorDtoToExternalAffiliationDto(RorDto data);
}
