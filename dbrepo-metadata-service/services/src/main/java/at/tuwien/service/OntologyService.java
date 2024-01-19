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

    /**
     * Finds all ontologies in the metadata database.
     *
     * @return The list of ontologies.
     */
    List<Ontology> findAll();

    /**
     * Finds all processable ontologies.
     *
     * @return The list of ontologies.
     */
    List<Ontology> findAllProcessable();

    /**
     * Finds an ontology in the metadata database with given id.
     *
     * @param id The ontology id.
     * @return The ontology, if successful.
     * @throws OntologyNotFoundException The ontology was not found in the metadata database.
     */
    Ontology find(Long id) throws OntologyNotFoundException;

    /**
     * Registers an ontology in the metadata database.
     *
     * @param data      The ontology data.
     * @param principal The user principal.
     * @return The created ontology, if successful.
     */
    Ontology create(OntologyCreateDto data, Principal principal);

    /**
     * Updates an ontology in the metadata database with given id.
     *
     * @param id   The ontology id.
     * @param data The ontology data.
     * @return The updated ontology, if successful.
     * @throws OntologyNotFoundException The ontology was not found in the metadata database.
     */
    Ontology update(Long id, OntologyModifyDto data) throws OntologyNotFoundException;

    /**
     * Unregisters an ontology in the metadata database with given id.
     *
     * @param id The ontology id.
     * @throws OntologyNotFoundException The ontology was not found in the metadata database.
     */
    void delete(Long id) throws OntologyNotFoundException;
}
