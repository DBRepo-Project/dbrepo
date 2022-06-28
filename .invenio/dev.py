#!/usr/bin/env python3
import re
import csv
import requests

doi = '10.5281/zenodo.5649276'
headers = {
    'Authorize': 'Bearer djCvqkoOW69keHajybZiwE8bBjyir2QSZOLKpAtc4S1Wp17KXgcHmMoWJwft' 
}

# Resolve DOI
response = requests.get('https://doi.org/' + doi)
id = re.findall('/([a-z0-9-]+)$', response.url)[0]
host = re.findall('^https?:\/\/([a-z0-9]+\.[a-z]+)', response.url)[0]
print("Resolved DOI to", host, "and record id", id)

# Find files
url = 'https://' + host + '/api/records/' + id
response = requests.get(url, headers=headers)
record = response.json()

# Write some .csv
i = 0
with open('./features.csv', 'w') as f:
    writer = csv.writer(f)
    writer.writerow(['key', 'size', 'link'])
    for file in record['files']:
        requests.get(file['links']['self'])
        print("... feature extract from", file['links']['self'])
        writer.writerow([file['key'], file['size'], file['links']['self']])
        i += 1
        if i > 10:
            break
print("Generated a feature .csv")
