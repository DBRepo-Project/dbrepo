package at.tuwien.service;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.OntologyNotFoundException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

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
     * @param ontologyId The ontology id.
     * @return The ontology, if successful.
     * @throws OntologyNotFoundException The ontology was not found in the metadata database.
     */
    Ontology find(UUID ontologyId) throws OntologyNotFoundException;

    Ontology find(String entityUri) throws OntologyNotFoundException;

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
     * @param ontology The ontology.
     * @param data     The ontology data.
     * @return The updated ontology, if successful.
     */
    Ontology update(Ontology ontology, OntologyModifyDto data);

    /**
     * Unregisters an ontology in the metadata database with given id.
     *
     * @param ontology The ontology.
     */
    void delete(Ontology ontology);
}
