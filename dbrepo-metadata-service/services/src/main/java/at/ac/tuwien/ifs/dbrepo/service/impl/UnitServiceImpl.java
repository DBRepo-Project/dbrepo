package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnUnit;
import at.ac.tuwien.ifs.dbrepo.core.exception.UnitNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.UnitRepository;
import at.ac.tuwien.ifs.dbrepo.service.UnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
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
