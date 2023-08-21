package at.tuwien.mapper;

import at.tuwien.api.maintenance.BannerMessageBriefDto;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageDto;
import at.tuwien.api.maintenance.BannerMessageTypeDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.entities.maintenance.BannerMessageType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BannerMessageMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BannerMessageMapper.class);

    BannerMessageDto bannerMessageToBannerMessageDto(BannerMessage data);

    BannerMessageBriefDto bannerMessageToBannerMessageBriefDto(BannerMessage data);

    BannerMessage bannerMessageCreateDtoToBannerMessage(BannerMessageCreateDto data);

    BannerMessageType bannerMessageTypeDtoToBannerMessageType(BannerMessageTypeDto data);

}
