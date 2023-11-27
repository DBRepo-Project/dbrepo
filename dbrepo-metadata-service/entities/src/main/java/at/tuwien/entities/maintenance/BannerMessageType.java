package at.tuwien.entities.maintenance;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum BannerMessageType {
    WARNING,
    ERROR,
    INFO;
}