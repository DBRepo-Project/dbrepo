#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat Dec  4 11:37:19 2021

@author: Cornelia Michlits
"""
import logging
import rdflib
import re

g = rdflib.Graph()
g.namespace_manager.bind('om', 'http://www.ontology-of-units-of-measure.org/resource/om-2/')
g.parse('ontologies/om-2.ttl', format='turtle')

om = rdflib.Namespace('http://www.ontology-of-units-of-measure.org/resource/om-2/')
rdf_schema = rdflib.Namespace('http://www.w3.org/2000/01/rdf-schema#')

f = rdflib.Graph()
f.namespace_manager.bind('qudt', 'http://qudt.org/2.1/vocab/unit')
f.parse('ontologies/VOCAB_QUDT-UNITS-ALL-v2.1.ttl', format='turtle')


# qudt = rdflib.Namespace('http://qudt.org/2.1/vocab/unit')

def list_units(string, offset=0) -> []:
    logging.info(f"list units for unit string {string}")
    if bool(re.match('^[a-zA-Z0-9\\s]+$', string)):
        l_query = """
        SELECT ?symbol ?name ?comment
        WHERE {
            ?unit om:symbol ?symbol .
            ?unit <http://www.w3.org/2000/01/rdf-schema#label> ?name .
            ?unit <http://www.w3.org/2000/01/rdf-schema#comment> ?comment .
            FILTER (regex(str(?unit),\"""" + string + """\","i") && lang(?name)="en")
            } LIMIT 10 OFFSET """ + str(offset)
        qres = g.query(l_query)
        units = list()
        for row in qres:
            units.append({"symbol": str(row.symbol), "name": str(row.name), "comment": str(row.comment)})
        return units
    else:
        return None


def list_concepts(string, offset=0) -> []:
    logging.info(f"list concepts for concepts string {string}")
    if bool(re.match('^[a-zA-Z0-9\\s]+$', string)):
        l_query = """
        SELECT ?symbol ?name ?comment
        WHERE {
            ?unit om:symbol ?symbol .
            ?unit <http://www.w3.org/2000/01/rdf-schema#label> ?name .
            ?unit <http://www.w3.org/2000/01/rdf-schema#comment> ?comment .
            FILTER (regex(str(?unit),\"""" + string + """\","i") && lang(?name)="en")
            } LIMIT 10 OFFSET """ + str(offset)
        qres = g.query(l_query)
        units = list()
        for row in qres:
            units.append({"symbol": str(row.symbol), "name": str(row.name), "comment": str(row.comment)})
        return units
    else:
        return None


def get_uri(name):
    logging.info(f"get url for concept name {name}")
    if bool(re.match('^[a-zA-Z0-9\\s]+$', name)):
        uri_query = """
        SELECT ?uri ?o
        WHERE {
            ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?o .
            FILTER regex(str(?o),\"^""" + name + """$\","i")
            } LIMIT 1
        """
        qres = g.query(uri_query)
        for row in qres:
            return {"uri": str(row.uri)}
    else:
        return None
