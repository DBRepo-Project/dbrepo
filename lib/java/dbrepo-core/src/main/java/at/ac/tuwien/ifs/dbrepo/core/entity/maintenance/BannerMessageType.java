package at.ac.tuwien.ifs.dbrepo.core.entity.maintenance;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum BannerMessageType {
    WARNING,
    ERROR,
    INFO
}