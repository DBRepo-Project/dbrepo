import re
import requests as rq

host = "https://test.researchdata.tuwien.ac.at"
file = "./resources/audio.wav"
token = "kJTJMtRKzmA0S92A9Jks7Y1OQbtRWElzgUrldXehDbeXESxIGQ0BQ6JFEFyg"

headers = {
    "Authorization": "Bearer " + token
}
filename = re.findall("([a-zA-z0-9_-]+\\.wav)", file)[0]

response = rq.post(host + "/api/records", json={
    "access": {
        "record": "public",
        "files": "public",
        "embargo": None
    },
    "files": {
        "enabled": True,
        "default_preview": None
    },
    "metadata": {
        "creators": [{
            "affiliations": [
                {
                    "name": "University of Ljubljana"
                }
            ],
            "person_or_org": {
                "type": "personal",
                "name": "M. Marolt",
                "identifiers": [
                    {
                        "scheme": "orcid",
                        "identifier": "0000-0002-0619-8789"
                    }
                ],
                "given_name": "Matija",
                "family_name": "Marolt"
            }
        }, {
            "affiliations": [
                {
                    "name": "University of Ljubljana"
                }
            ],
            "person_or_org": {
                "type": "personal",
                "name": "C. Bohak",
                "identifiers": [
                    {
                        "scheme": "orcid",
                        "identifier": "0000-0002-9015-2897"
                    }
                ],
                "given_name": "Ciril",
                "family_name": "Bohak"
            }
        }, {
            "affiliations": [
                {
                    "name": "University of Ljubljana"
                }
            ],
            "person_or_org": {
                "type": "personal",
                "name": "A. Kavčič",
                "given_name": "Alenka",
                "family_name": "Kavčič"
            }
        }, {
            "affiliations": [
                {
                    "name": "University of Ljubljana"
                }
            ],
            "person_or_org": {
                "type": "personal",
                "name": "M. Pesek",
                "identifiers": [
                    {
                        "scheme": "orcid",
                        "identifier": "0000-0001-9101-0471"
                    }
                ],
                "given_name": "Matevž",
                "family_name": "Pesek"
            }
        }],
        "title": "The SeFiRe field recording dataset",
        "resource_type": {
            "id": "sound"
        },
        "publication_date": "2019-01-28"
    }
}, headers=headers).json()
print(response)
record_id = response["id"]

# announce
response = rq.post(host + "/api/records/" + record_id + "/draft/files", json=[{
    "key": filename
}], headers=headers).json()
print(response)

# upload
with open(file, mode='rb') as f:
    response = rq.put(host + "/api/records/" + record_id + "/draft/files/" + filename + "/content", data=f.read(),
                      headers=headers).json()
print(response)

# commit
response = rq.post(host + "/api/records/" + record_id + "/draft/files/" + filename + "/commit", headers=headers).json()
print(response)
print()

# publish
response = rq.post(host + "/api/records/" + record_id + "/draft/actions/publish", headers=headers).json()
print(response)
print()