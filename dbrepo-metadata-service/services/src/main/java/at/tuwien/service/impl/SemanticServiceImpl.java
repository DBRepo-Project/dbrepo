package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.UnitNotFoundException;
import at.tuwien.repository.mdb.*;
import at.tuwien.service.SemanticService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
public class SemanticServiceImpl implements SemanticService {

    private final DatabaseRepository databaseRepository;

    @Autowired
    public SemanticServiceImpl(DatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnConcept> findAllConcepts() {
        return databaseRepository.findAll()
                .stream()
                .map(Database::getTables)
                .flatMap(List::stream)
                .map(Table::getColumns)
                .flatMap(List::stream)
                .map(TableColumn::getConcept)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnUnit> findAllUnits() {
        return databaseRepository.findAll()
                .stream()
                .map(Database::getTables)
                .flatMap(List::stream)
                .map(Table::getColumns)
                .flatMap(List::stream)
                .map(TableColumn::getUnit)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnUnit findUnit(String uri) throws UnitNotFoundException {
        final Optional<TableColumnUnit> optional = databaseRepository.findAll()
                .stream()
                .map(Database::getTables)
                .flatMap(List::stream)
                .map(Table::getColumns)
                .flatMap(List::stream)
                .map(TableColumn::getUnit)
                .filter(Objects::nonNull)
                .filter(u -> u.getUri().equals(uri))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find unit with uri {} in metadata database", uri);
            throw new UnitNotFoundException("Failed to find unit with uri " + uri);
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnConcept findConcept(String uri) throws ConceptNotFoundException {
        final Optional<TableColumnConcept> optional = databaseRepository.findAll()
                .stream()
                .map(Database::getTables)
                .flatMap(List::stream)
                .map(Table::getColumns)
                .flatMap(List::stream)
                .map(TableColumn::getConcept)
                .filter(Objects::nonNull)
                .filter(c -> c.getUri().equals(uri))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find concept with uri {} in metadata database", uri);
            throw new ConceptNotFoundException("Failed to find concept with uri " + uri);
        }
        return optional.get();
    }

}
