#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat Dec  4 11:37:19 2021

@author: Cornelia Michlits
@author: Martin Weise
"""
import logging
import rdflib
import re
import requests as rq


class List:

    def __init__(self, offline=False):
        rdflib.Namespace('http://www.ontology-of-units-of-measure.org/resource/om-2/')

        self.g = rdflib.Graph()
        self.g.namespace_manager.bind('om', 'http://www.ontology-of-units-of-measure.org/resource/om-2/')
        self.g.namespace_manager.bind('wd', 'http://www.wikidata.org/entity/')
        self.g.namespace_manager.bind('wdt', 'http://www.wikidata.org/prop/direct/')
        self.g.namespace_manager.bind('schema', 'http://schema.org/')

        if not offline:
            # ontology of measure
            rdf = rq.get('http://www.ontology-of-units-of-measure.org/data/om-2/',
                         headers={'Accept': 'application/rdf+xml'})
            rdf.raise_for_status()
            self.g.parse(data=rdf.text, format='xml')

            # wikidata
            rdf = rq.get('https://query.wikidata.org/sparql',
                         headers={'Accept': 'application/rdf+xml'})
            rdf.raise_for_status()
        else:
            self.g.parse('ontologies/om-2.rdf', format='xml')

    def list_units(self, name, offset=0) -> []:
        name = name.lower()
        logging.info(f"list units for unit name {name}")
        l_query = """SELECT DISTINCT ?item ?symbol ?name ?comment
        WHERE {
            ?unit om:symbol ?symbol .
            ?unit rdfs:label ?name .
            ?unit rdfs:comment ?comment .
            ?unit rdf:type om:Unit .
            FILTER(CONTAINS(LCASE(?name), \"""" + name + """\"@en)) .
            FILTER(LANG(?name) = "en") .
        } LIMIT 10 OFFSET """ + str(offset)
        qres = self.g.query(l_query)
        units = list()
        for row in qres:
            units.append({"uri": str(row.item), "symbol": str(row.symbol), "name": str(row.name), "comment": str(row.comment)})
        return units

    def list_concepts(self, name) -> []:
        name = name.lower()
        logging.info(f"list concepts for concepts name {name}")
        l_query = """SELECT DISTINCT ?item ?name ?comment
        WHERE { 
            SERVICE <https://query.wikidata.org/sparql> {
                SELECT ?item ?name ?comment
                WHERE {
                    ?item wdt:P279* wd:Q1183543 .
                    ?item rdfs:label ?name .
                    ?item schema:description ?comment .
                    FILTER(LANG(?comment) = "en") .
                    FILTER(LANG(?name) = "en") .
                    FILTER(CONTAINS(LCASE(?name), \"""" + name + """\"@en)) . 
                }
            }
        }"""
        qres = self.g.query(l_query)
        units = list()
        for row in qres:
            units.append({"uri": str(row.item), "name": str(row.name), "comment": str(row.comment)})
        return units

    def get_unit_uri(self, name) -> {}:
        name = name.replace('(', '\\\\(')
        name = name.replace(')', '\\\\)')
        logging.info(f"get url for unit name {name}")
        uri_query = """SELECT ?uri
        WHERE {
            ?uri rdfs:label ?o .
            FILTER regex(str(?o),\"^""" + name + """$\","i") .
            } LIMIT 1
        """
        qres = self.g.query(uri_query)
        for row in qres:
            print(f"res: uri={row.uri}")
            return {"uri": str(row.uri)}

    def get_concept_uri(self, name) -> {}:
        name = name.lower()
        logging.info(f"get url for concept name {name}")
        uri_query = """SELECT DISTINCT ?item ?name ?comment
        WHERE { 
            SERVICE <https://query.wikidata.org/sparql> {
                SELECT ?item ?name ?comment
                WHERE {
                    ?item wdt:P279* wd:Q1183543 .
                    ?item rdfs:label ?name .
                    ?item schema:description ?comment .
                    FILTER(LANG(?comment) = "en") .
                    FILTER(LANG(?name) = "en") .
                    FILTER regex(?name, \"^""" + name + """$\", "i") .
                }
            }
        }"""
        qres = self.g.query(uri_query)
        for row in qres:
            print(f"res: uri={row.item}")
            return {"uri": str(row.item)}
