package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessage;
import at.ac.tuwien.ifs.dbrepo.core.exception.MessageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.repository.BannerMessageRepository;
import at.ac.tuwien.ifs.dbrepo.service.BannerMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class BannerMessageServiceImpl implements BannerMessageService {

    private final MetadataMapper metadataMapper;
    private final BannerMessageRepository bannerMessageRepository;

    @Autowired
    public BannerMessageServiceImpl(MetadataMapper metadataMapper,
                                    BannerMessageRepository bannerMessageRepository) {
        this.metadataMapper = metadataMapper;
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
    public BannerMessage find(UUID id) throws MessageNotFoundException {
        final Optional<BannerMessage> optional = bannerMessageRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find banner message with id {}", id);
            throw new MessageNotFoundException("Failed to find banner message with id " + id);
        }
        return optional.get();
    }

    @Override
    public BannerMessage create(BannerMessageCreateDto data) {
        final BannerMessage entity = metadataMapper.bannerMessageCreateDtoToBannerMessage(data);
        final BannerMessage message = bannerMessageRepository.save(entity);
        log.info("Created banner message with id {}", message.getId());
        return message;
    }

    @Override
    public BannerMessage update(BannerMessage message, BannerMessageUpdateDto data) {
        message.setMessage(data.getMessage());
        message.setDisplayEnd(data.getDisplayEnd());
        message.setDisplayStart(data.getDisplayStart());
        message.setType(metadataMapper.bannerMessageTypeDtoToBannerMessageType(data.getType()));
        message = bannerMessageRepository.save(message);
        log.info("Updated banner message with id {}", message.getId());
        return message;
    }

    @Override
    public void delete(BannerMessage message) {
        bannerMessageRepository.deleteById(message.getId());
        log.info("Deleted banner message with id {}", message.getId());
    }

}
