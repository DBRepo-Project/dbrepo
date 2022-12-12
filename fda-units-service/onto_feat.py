import re
import glob
import sys
import os
from pathlib import Path
import rdflib

ALLOWED_EXTENSIONS = {'ttl', 'nt'}

ONTOLOGIES_DIRECTORY = 'ontologies'

ONTOLOGY_EXTENSIONS = {'.ttl': 'turtle', '.nt': 'nt'}

def search_ontologies(query, limit=sys.maxsize, offset=0):
    if not bool(re.match('^[a-zA-Z0-9\-\\\s]+$', query)):
        return None
    ontology_files = glob.glob(ONTOLOGIES_DIRECTORY + "/*")
    matches = []
    for file in ontology_files:
        format = ONTOLOGY_EXTENSIONS[Path(file).suffix]
        g = rdflib.Graph()
        g.parse(file, format=format)
        l_query = """
                SELECT ?s ?p ?o
                WHERE{
                    ?s ?p ?o .
                    FILTER regex(str(?s),\"""" + query + """\","i")
                                 }LIMIT """ + str(limit) + """ OFFSET """ + str(offset)
        qres1 = g.query(l_query)
        for row in qres1:
            matches.append({"S-URI": str(row.s), "P": str(row.p), "O": str(row.o)})
    return matches
def setup_ontology_dir():
    if not os.path.exists(ONTOLOGIES_DIRECTORY):
        os.mkdir(ONTOLOGIES_DIRECTORY)
def list_ontologies():
    setup_ontology_dir()
    return list(map(lambda filename: Path(filename).stem, glob.glob(ONTOLOGIES_DIRECTORY + "/*")))

def ontology_exists(name):
    setup_ontology_dir()
    return name in list_ontologies()

def get_ontology(name):
    setup_ontology_dir()
    files = glob.glob(ONTOLOGIES_DIRECTORY + "/" + name + ".*")
    if len(files) == 0:
        return None
    with open(files[0]) as f:
        return f.read()

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS