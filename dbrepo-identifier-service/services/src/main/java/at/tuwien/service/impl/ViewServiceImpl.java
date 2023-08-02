package at.tuwien.service.impl;

import at.tuwien.entities.database.View;
import at.tuwien.exception.ViewNotFoundException;
import at.tuwien.repository.mdb.ViewRepository;
import at.tuwien.service.ViewService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
public class ViewServiceImpl implements ViewService {

    private final ViewRepository viewRepository;

    @Autowired
    public ViewServiceImpl(ViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    @Override
    public View findById(Long id) throws ViewNotFoundException {
        final Optional<View> optional = viewRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find view with id: {}", id);
            throw new ViewNotFoundException("Failed to find view with id: " + id);
        }
        return optional.get();
    }
}
