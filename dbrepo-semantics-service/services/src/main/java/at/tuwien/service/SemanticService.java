package at.tuwien.service;

import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;

public interface SemanticService {
    TableColumnConcept saveConcept(ConceptSaveDto data);

    TableColumnUnit saveUnit(UnitSaveDto data);
}
