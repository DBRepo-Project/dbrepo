#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Dec  2 23:31:39 2021

@author: Cornelia Michlits
"""
import rdflib
import requests as rq

rdf = rq.get('http://www.ontology-of-units-of-measure.org/resource/om-2', headers={'Accept': 'application/rdf+xml'})
rdf.raise_for_status()

g = rdflib.Graph()
g.namespace_manager.bind('om', 'http://www.ontology-of-units-of-measure.org/resource/om-2/')
g.parse(data=rdf.text, format='xml')

om = rdflib.Namespace('http://www.ontology-of-units-of-measure.org/resource/om-2/')
rdf_schema = rdflib.Namespace('http://www.w3.org/2000/01/rdf-schema#')


# rdf = rq.get('http://qudt.org/2.1/vocab/unit', headers={'Accept': 'text/turtle'})
# rdf.raise_for_status()
#
# f = rdflib.Graph()
# f.namespace_manager.bind('qudt', 'http://qudt.org/2.1/vocab/unit')
# f.parse(data=rdf.text, format='turtle')

_exhausted = object()


def validator(value):
    # input str
    tmp = str(om) + value
    t_uri = rdflib.term.URIRef(tmp)
    if next(g.triples((t_uri, None, om.Unit)), _exhausted) is _exhausted and next(
            g.triples((t_uri, None, om.PrefixedUnit)), _exhausted) is _exhausted and next(
            g.triples((t_uri, None, None)), _exhausted) is _exhausted:
        return False
    else:
        return True


def stringmapper(thisstring):
    if ' ' in thisstring:
        return thisstring.split(" ", 1)[0].lower() + thisstring.split(" ", 1)[1].title().replace(" ", "")
    else:
        return thisstring
