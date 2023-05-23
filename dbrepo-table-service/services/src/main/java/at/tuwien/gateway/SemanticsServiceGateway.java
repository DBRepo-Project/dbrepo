package at.tuwien.gateway;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.exception.SemanticEntityPersistException;

public interface SemanticsServiceGateway {
    ConceptDto saveConcept(ConceptSaveDto data, String authorization) throws SemanticEntityPersistException;

    UnitDto saveUnit(UnitSaveDto data, String authorization) throws SemanticEntityPersistException;
}
