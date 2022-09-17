package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;

import java.util.List;

public interface IdentifierService {
    List<Identifier> findAll();
}
