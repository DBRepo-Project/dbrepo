from dotenv import load_dotenv
from pandas.core.interchange.dataframe_protocol import DataFrame
load_dotenv()
from dbrepo.RestClient import RestClient

database_id = 37
client = RestClient()

# TABLES

# test_result_chloride_ageing = client.get_table_data(database_id, 615)
# test_result_chloride = client.get_table_data(database_id, 614)
# test_result_carb_nat = client.get_table_data(database_id, 613)
# test_result_carb_acc = client.get_table_data(database_id, 612)
# source_mat_analysis = client.get_table_data(database_id, 611)
# source_mat = client.get_table_data(database_id, 610)
# pruefstelle = client.get_table_data(database_id, 609)
# mix_recipe = client.get_table_data(database_id, 607)
# mix = client.get_table_data(database_id, 606)
betonvariante = client.get_table_data(database_id, 604)

# optional: export table data as .csv
# betonvariante.to_csv('betonvariante.csv')

# optional: update table data from .csv
# client.import_table_data(database_id, 604, 'my_data.csv')

# optional: update table data from `pandas` DataFrame
# df = DataFrame()
# client.import_table_data(database_id, 604, df)

# VIEWS

beton_acc = client.get_view_data(database_id, 70)

# optional: export view data as .csv
# beton_acc.to_csv('beton_acc.csv')

# SUBSETS

subset = client.create_subset(database_id, "SELECT m.mix, date_production, b.name, ta.k_ac_mm_root_day, ta.test_procedure, ta.mean_depth_t0_mm FROM mix m JOIN betonvariante b ON b.id = m.betonvariante_id JOIN test_result_carb_acc ta ON m.id = ta.mix_id")
