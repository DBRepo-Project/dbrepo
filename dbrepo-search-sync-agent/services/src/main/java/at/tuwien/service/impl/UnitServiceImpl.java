package at.tuwien.service.impl;

import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.repository.mdb.UnitRepository;
import at.tuwien.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;

    @Autowired
    public UnitServiceImpl(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnUnit> findAll() {
        return unitRepository.findAll();
    }
}
