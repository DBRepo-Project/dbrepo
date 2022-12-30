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
        rdflib.Namespace('http://www.w3.org/2000/01/rdf-schema')

        self.g = rdflib.Graph()
        self.g.namespace_manager.bind('om', 'http://www.ontology-of-units-of-measure.org/resource/om-2/')

        self.w = rdflib.Graph()
        self.w.namespace_manager.bind('wd', 'https://www.wikidata.org/wiki/')

        if not offline:
            # ontology of measure
            rdf = rq.get('http://www.ontology-of-units-of-measure.org/resource/om-2/',
                         headers={'Accept': 'application/rdf+xml'})
            rdf.raise_for_status()
            self.g.parse(data=rdf.text, format='xml')

            # wikidata
            rdf = rq.get('https://query.wikidata.org/sparql',
                         headers={'Accept': 'application/rdf+xml'})
            rdf.raise_for_status()
            self.w.parse(data=rdf.text, format='xml')
        else:
            self.g.parse('ontologies/om-2.rdf', format='xml')
            self.w.parse('ontologies/wikidata.rdf', format='xml')

        #
        # self.f = rdflib.Graph()
        # self.f.namespace_manager.bind('qudt', 'http://qudt.org/2.1/vocab/unit')
        # self.f.parse(data=rdf.text, format='turtle')

        # self.qudt = rdflib.Namespace('http://qudt.org/2.1/vocab/unit')

    def list_units(self, string, offset=0) -> []:
        string = string.replace('(', '\\\\(')
        string = string.replace(')', '\\\\)')
        logging.info(f"list units for unit string {string}")
        l_query = """SELECT ?symbol ?name ?comment
        WHERE {
            ?unit om:symbol ?symbol .
            ?unit rdfs:label ?name .
            ?unit rdfs:comment ?comment .
            ?type rdfs:subClassOf* :Unit.
            FILTER (regex(str(?unit),\"""" + string + """\","i") && lang(?name)="en")
            } LIMIT 10 OFFSET """ + str(offset)
        qres = self.g.query(l_query)
        units = list()
        for row in qres:
            units.append({"symbol": str(row.symbol), "name": str(row.name), "comment": str(row.comment)})
        return units

    def list_concepts(self, string, offset=0) -> []:
        string = string.replace('(', '\\\\(')
        string = string.replace(')', '\\\\)')
        logging.info(f"list concepts for concepts string {string}")
        l_query = """SELECT DISTINCT ?name
        WHERE {
            ?label rdfs:label ?name .
            FILTER (lang(?name) = 'en').
            FILTER (regex(str(?name),\"^""" + string + """\","i") && lang(?name)="en")
            } LIMIT 10 OFFSET """ + str(offset)
        qres = self.w.query(l_query)
        units = list()
        for row in qres:
            units.append({"symbol": str(row.symbol), "name": str(row.name), "comment": str(row.comment)})
        return units

    def get_unit_uri(self, name) -> {}:
        name = name.replace('(', '\\\\(')
        name = name.replace(')', '\\\\)')
        logging.info(f"get url for unit name {name}")
        uri_query = """SELECT ?uri
        WHERE {
            ?uri rdfs:label ?o .
            FILTER regex(str(?o),\"^""" + name + """$\","i")
            } LIMIT 1
        """
        qres = self.g.query(uri_query)
        for row in qres:
            print(f"res: uri={row.uri}")
            return {"uri": str(row.uri)}

    def get_concept_uri(self, name) -> {}:
        name = name.replace('(', '\\\\(')
        name = name.replace(')', '\\\\)')
        logging.info(f"get url for concept name {name}")
        uri_query = """SELECT ?uri
        WHERE {
            ?uri rdfs:label ?o .
            FILTER regex(str(?o),\"^""" + name + """$\","i")
            } LIMIT 1
        """
        qres = self.w.query(uri_query)
        for row in qres:
            print(f"res: uri={row.uri}")
            return {"uri": str(row.uri)}
