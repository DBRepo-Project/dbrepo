package at.tuwien.service;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.KeycloakRemoteException;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.UserNotFoundException;

import java.security.Principal;
import java.util.List;

public interface OntologyService {
    List<Ontology> findAll();

    List<Ontology> findAllProcessable();

    Ontology find(Long id) throws OntologyNotFoundException;

    Ontology create(OntologyCreateDto data, Principal principal) throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException;

    Ontology update(Long id, OntologyModifyDto data) throws OntologyNotFoundException;

    void delete(Long id) throws OntologyNotFoundException;
}
