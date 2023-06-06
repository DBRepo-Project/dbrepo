package at.tuwien.service.impl;

import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.exception.BannerMessageNotFoundException;
import at.tuwien.mapper.BannerMessageMapper;
import at.tuwien.repository.mdb.BannerMessageRepository;
import at.tuwien.service.BannerMessageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class BannerMessageServiceImpl implements BannerMessageService {

    private final BannerMessageMapper bannerMessageMapper;
    private final BannerMessageRepository bannerMessageRepository;

    @Autowired
    public BannerMessageServiceImpl(BannerMessageMapper bannerMessageMapper,
                                    BannerMessageRepository bannerMessageRepository) {
        this.bannerMessageMapper = bannerMessageMapper;
        this.bannerMessageRepository = bannerMessageRepository;
    }

    @Override
    public List<BannerMessage> findAll() {
        return bannerMessageRepository.findAll();
    }

    @Override
    public List<BannerMessage> getActive() {
        return bannerMessageRepository.findByActive();
    }

    @Override
    public BannerMessage find(Long id) throws BannerMessageNotFoundException {
        final Optional<BannerMessage> optional = bannerMessageRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find banner message with id {}", id);
            throw new BannerMessageNotFoundException("Failed to find banner message with id " + id);
        }
        return optional.get();
    }

    @Override
    public BannerMessage create(BannerMessageCreateDto data) {
        final BannerMessage entity = bannerMessageMapper.bannerMessageCreateDtoToBannerMessage(data);
        final BannerMessage message = bannerMessageRepository.save(entity);
        log.info("Created banner message with id {}", message.getId());
        return message;
    }

    @Override
    public BannerMessage update(Long id, BannerMessageUpdateDto data) throws BannerMessageNotFoundException {
        final BannerMessage entity = find(id);
        entity.setMessage(data.getMessage());
        entity.setDisplayEnd(data.getDisplayEnd());
        entity.setDisplayStart(data.getDisplayStart());
        entity.setType(bannerMessageMapper.bannerMessageTypeDtoToBannerMessageType(data.getType()));
        final BannerMessage message = bannerMessageRepository.save(entity);
        log.info("Updated banner message with id {}", message.getId());
        return message;
    }

    @Override
    public void delete(Long id) throws BannerMessageNotFoundException {
        find(id);
        bannerMessageRepository.deleteById(id);
        log.info("Deleted banner message with id {}", id);
    }

}
