import csv
import time

with open('./resources/ugz_ogd_air_h1_2021.csv', mode='r', encoding='utf-8-sig') as f:
    csv_reader = csv.reader(f, delimiter=',', quotechar='"')
    for row in csv_reader:
        payload = {'date': row[0], 'location': row[1], 'parameter': row[2], 'interval': row[3], 'unit': row[4],
                   'value': row[5], 'status': row[6]}
        print('sending', payload, '...')
        time.sleep(5)
