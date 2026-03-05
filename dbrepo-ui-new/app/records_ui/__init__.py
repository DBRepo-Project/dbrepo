import json

import pandas as pd

from flask import Flask, render_template, Blueprint, request, jsonify
import dbrepo.RestClient
from flask_babelex import Babel, gettext, lazy_gettext

from citeproc import CitationStylesStyle, CitationStylesBibliography
from citeproc import Citation, CitationItem
from citeproc.source.json import CiteProcJSON
from citeproc import formatter


def generate_citation(item):
    styles = ["apa", "bibtex", "ieee"]
    results = {}

    for style_name in styles:
        bib_source = CiteProcJSON([item])
        style = CitationStylesStyle(style_name, validate=False)
        bibliography = CitationStylesBibliography(style, bib_source, formatter.html)

        citation = Citation([CitationItem(item["id"])])
        bibliography.register(citation)

        formatted = str(bibliography.bibliography()[0])
        results[style_name] = formatted

    return results


repo = dbrepo.RestClient.RestClient(endpoint='https://dbrepo.datalab.tuwien.ac.at', username='338563')

records_bp = Blueprint('records', __name__, template_folder='templates', static_folder='static',
                       static_url_path='/static/admin')


@records_bp.route('/database/<db_id>')
def show_record(db_id):
    doi = request.args.get('doi', 0)


    database = repo.get_database(database_id=db_id)
    if len(database.views) > 0:
        view = repo.get_view(database_id=db_id, view_id=database.views[0].id)
    else:
        view = None
    # view_data = repo.get_view_data(database_id=db_id, view_id=database.views[1].id, page=1, size=10).to_dict(orient='records')
    # queries = repo.get_queries(database_id=db_id)

    identifier = database.identifiers[0]
    if doi != 0:
        identifier = get_identifier_by_doi(database.identifiers, doi)
    # PLACEHOLDERS
    queries = []
    view_data = pd.DataFrame({"placeholder": []})

    authors_list = [{"literal": c.creator_name} for c in identifier.creators]

    citation_item = {
        "id": "dataset-1",
        "type": "dataset",
        "title": database.name,
        "author": authors_list,
        "issued": {"date-parts": [[identifier.publication_year]]}
    }

    citation_html = generate_citation(citation_item)
    print(citation_html)

    view_data = view_data.to_dict(orient="records")
    return render_template('records/detail.html', database=database, view=view, data=view_data,
                           doi_id=0, identifier=identifier, queries=queries, citations=citation_html)


@records_bp.route('/view/<database_id>/<view_id>')
def show_view_record(database_id, view_id):
    view = repo.get_view(database_id=database_id, view_id=view_id)
   # view_data = repo.get_view_data(database_id=database_id, view_id=view_id, page=1, size=10).to_dict(orient='records')
    database = repo.get_database(database_id=database_id)
    view_data = pd.DataFrame({"placeholder": []})

    return render_template('view_records/detail.html', database=database, metadata=view,
                           data=view_data, type="view", page=1, total_pages=10)

@records_bp.route('/subset/<database_id>/<subset_id>')
def show_subset_record(database_id, subset_id):
    subset = repo.get_subset(database_id=database_id, subset_id=subset_id)
    subset_data = repo.get_subset_data(database_id=database_id, subset_id=subset_id).to_dict(orient='records')
    database = repo.get_database(database_id=database_id)


    return render_template('view_records/detail.html', database=database, metadata=subset,
                           data=subset_data, type="subset")


@records_bp.route('/get-data', methods=['GET'])
def get_data():
    database_id = request.args.get('database_id')
    view_id = request.args.get('id')

    view_data = repo.get_view_data(database_id=database_id, view_id=view_id, page=1, size=10).to_dict(orient='records')
    print(view_data)
    return jsonify(view_data)  # Return data as JSON


@records_bp.route('/get-subset-data', methods=['GET'])
def get_subset_data():
    databse_id = request.args.get('database_id')
    subset_id = request.args.get('id')
    view_data = repo.get_subset_data(database_id=databse_id, subset_id=subset_id).to_dict(orient='records')

    return jsonify(view_data)  # Return data as JSON


view_records_bp = Blueprint('view_records', __name__, template_folder='templates', static_folder='static',
                            static_url_path='/static/admin')


def get_identifier_by_doi(identifiers, doi):
    for identifier in identifiers:
        if identifier.doi == doi:
            return identifier
    return None
