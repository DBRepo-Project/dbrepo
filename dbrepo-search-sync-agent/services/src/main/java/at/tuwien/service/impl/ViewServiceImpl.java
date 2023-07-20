package at.tuwien.service.impl;

import at.tuwien.entities.database.View;
import at.tuwien.repository.mdb.ViewRepository;
import at.tuwien.service.ViewService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
public class ViewServiceImpl implements ViewService {

    private final ViewRepository viewRepository;

    @Autowired
    public ViewServiceImpl(ViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<View> findAll() {
        return viewRepository.findAll();
    }

}
