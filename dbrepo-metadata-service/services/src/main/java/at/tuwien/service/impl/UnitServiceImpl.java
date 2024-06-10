package at.tuwien.service.impl;

import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.UnitNotFoundException;
import at.tuwien.repository.UnitRepository;
import at.tuwien.service.UnitService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;

    @Autowired
    public UnitServiceImpl(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional
    public TableColumnUnit create(TableColumnUnit unit) {
        return unitRepository.save(unit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnUnit> findAll() {
        return unitRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnUnit find(String uri) throws UnitNotFoundException {
        final Optional<TableColumnUnit> optional = unitRepository.findByUri(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find unit with uri {} in metadata database", uri);
            throw new UnitNotFoundException("Failed to find unit in metadata database");
        }
        return optional.get();
    }

}
