package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;

import java.util.List;

public interface IdentifierService {
    List<Identifier> findAll();

    Identifier find(Long id) throws IdentifierNotFoundException;
}
