package at.tuwien.service;

import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.exception.BannerMessageNotFoundException;

import java.util.List;

public interface BannerMessageService {
    List<BannerMessage> findAll();

    List<BannerMessage> getActive();

    BannerMessage find(Long id) throws BannerMessageNotFoundException;

    BannerMessage create(BannerMessageCreateDto data);

    BannerMessage update(Long id, BannerMessageUpdateDto data) throws BannerMessageNotFoundException;

    void delete(Long id) throws BannerMessageNotFoundException;
}
