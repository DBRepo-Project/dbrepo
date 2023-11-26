package at.tuwien.converters;

import at.tuwien.entities.maintenance.BannerMessageType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BannerMessageTypeConverter implements AttributeConverter<BannerMessageType, String> {

    @Override
    public String convertToDatabaseColumn(BannerMessageType bannerMessageType) {
        if (bannerMessageType == null) {
            return null;
        }
        return bannerMessageType.name()
                .toLowerCase();
    }

    @Override
    public BannerMessageType convertToEntityAttribute(String bannerMessageType) {
        if (bannerMessageType == null) {
            return null;
        }
        return BannerMessageType.valueOf(bannerMessageType.toUpperCase());
    }
}
