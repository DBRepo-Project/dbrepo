"""
run the tests via 'pytest' or 'pipenv run pytest'

 if you want to run the test propperly, make sure to follow this list:
 * run 'pipenv run python3 run_testindicies.py' to start the test containers. You see the port number in the output.
 * change the config_class in app/__init__.py to 'TestConfig' instead of 'Config'
 * run pipenv run flask run --debug --port 4000
 * enter the port number manually (you prolly have to do that twice if you start it for the first time)
 * run the tests via 'pytest' or 'pipenv run pytest'
"""
import requests
def send_request(path, data):
    url = f"http://localhost:4000/api/search{path}"
    response = requests.post(url, json=data)
    if response.status_code == 200:
        return response.json()
    else:
        raise Exception(response.json())


def test_textsearch():
    print("search for entries that contain the word 'measurement data'")
    data = {"search_term": "measurement data"}
    result = send_request("", data)
    docIDs = [hit["_source"]["docID"] for hit in result["hits"]["hits"]]
    assert docIDs == [2]


def test_timerange():
    print("search for entries that have been created between January and September of 2023")
    data = {"t1":"2023-01-01",
                  "t2":"2023-09-09"}
    result = send_request("", data)
    docIDs = [hit["_source"]["docID"] for hit in result["hits"]["hits"]]
    assert docIDs == [1, 2]


def test_keywords():
    print("Search for entries form the user 'max")
    data = {"field": "author", "value": "max"}
    result = send_request("", data)
    docIDs = [hit["_source"]["docID"] for hit in result["hits"]["hits"]]
    assert docIDs == [2]

