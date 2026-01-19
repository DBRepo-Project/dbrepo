import json

from flask import Flask, render_template, Blueprint, request, jsonify
import dbrepo.RestClient
from flask_babelex import Babel, gettext, lazy_gettext


repo = dbrepo.RestClient.RestClient(endpoint='https://dbrepo.datalab.tuwien.ac.at', username='338563')

records_bp = Blueprint('records', __name__, template_folder='templates', static_folder='static',
                       static_url_path='/static/admin')


@records_bp.route('/database/<db_id>')
def show_record(db_id):
    database = repo.get_database(database_id=db_id)
    view = repo.get_view(database_id=db_id, view_id=database.views[0].id)
    view_data = repo.get_view_data(database_id=db_id, view_id=database.views[1].id, page=1, size=10).to_dict(orient='records')
    # queries = repo.get_queries(database_id=db_id)
    queries = []
    return render_template('records/detail.html', database=database, view=view, data=view_data,
                           doi_id=0, queries=queries)


@records_bp.route('/view/<database_id>/<view_id>')
def show_view_record(database_id, view_id):
    view = repo.get_view(database_id=database_id, view_id=view_id)
    view_data = repo.get_view_data(database_id=database_id, view_id=view_id, page=1, size=10).to_dict(orient='records')
    database = repo.get_database(database_id=database_id)

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
