import json

from flask import Flask, render_template, Blueprint, request, jsonify
import dbrepo.RestClient

app = Flask(__name__)

repo = dbrepo.RestClient.RestClient(endpoint='https://dbrepo1.ec.tuwien.ac.at')



records_bp = Blueprint('records', __name__, template_folder='templates', static_folder='static', static_url_path='/static/admin')


@records_bp.route('/database/<int:db_id>/<int:doi_id>/<int:view_id>')
def show_record(db_id, doi_id, view_id):
    database = repo.get_database(database_id=db_id)
    table = repo.get_table(database_id=db_id, table_id=13)
    table_data = repo.get_table_data(database_id=db_id, table_id=13, page=0, df=True).to_dict(orient='records')
    view_data = repo.get_view_data(database_id=db_id, view_id=view_id, df=True).to_dict(orient='records')

    views = database.views
    print(database.container)

    return render_template('records/detail.html', database=database, table=table,
                           table_data=table_data, views=views, view_data=view_data, doi_id=doi_id)


@records_bp.route('/get-data', methods=['GET'])
def get_data():
    view_id = request.args.get('id')
    view_data = repo.get_view_data(database_id=7, view_id=view_id, df=True).to_dict(orient='records')

    return jsonify(view_data)


view_records_bp = Blueprint('view_records', __name__, template_folder='templates', static_folder='static', static_url_path='/static/admin')
@records_bp.route('/view')
def show_view_record():

    view = repo.get_view(database_id=7, view_id=26)
    view_data = repo.get_view_data(database_id=7, view_id=26).to_dict(orient='records')
    database = repo.get_database(database_id=7)


    return render_template('view_records/detail.html', database=database, view=view, view_data=view_data, table_id=0)


#start with > python app.py
