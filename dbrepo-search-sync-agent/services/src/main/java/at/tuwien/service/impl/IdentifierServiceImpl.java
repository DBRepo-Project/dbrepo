package at.tuwien.service.impl;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final IdentifierRepository identifierRepository;

    @Autowired
    public IdentifierServiceImpl(IdentifierRepository identifierRepository) {
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll() {
        return identifierRepository.findAll();
    }
}
