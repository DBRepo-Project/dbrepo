#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Nov  9 11:25:52 2022

@author: Cornelia Michlits
"""

import rdflib
import re
import sys
g = rdflib.Graph()
g.namespace_manager.bind('iaq', 'https://w3id.org/digitalconstruction/IndoorAirQuality#')
g.parse('onto/iaq.nt', format='nt')

f = rdflib.Graph()
f.namespace_manager.bind('geom', 'http://data.ign.fr/def/geometrie')
f.parse('onto/def--geometrie.ttl', format='turtle')

h = rdflib.Graph()
h.namespace_manager.bind('op', 'http://environment.data.gov.au/def/op')
h.parse('onto/def--op.nt', format='nt')

def get_concept(string,limit=sys.maxsize, offset=0):
    if bool(re.match('^[a-zA-Z0-9\-\\\s]+$',string)):
        l_query = """
        SELECT ?s ?p ?o
        WHERE{
            ?s ?p ?o .
            FILTER regex(str(?s),\""""+string+"""\","i")
                         }LIMIT """+str(limit)+""" OFFSET """+str(offset)
        qres1 = g.query(l_query)
        res = list()
        for row in qres1:
            res.append({"S-URI": str(row.s), "P": str(row.p), "O": str(row.o)})
        qres2 = f.query(l_query)
        for row in qres2:
            res.append({"S-URI": str(row.s), "P": str(row.p), "O": str(row.o)})
        return res
        qres3 = h.query(l_query)
        for row in qres3:
            res.append({"S-URI": str(row.s), "P": str(row.p), "O": str(row.o)})
        return res
    else:
        return None