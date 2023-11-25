"""
run the tests via 'pytest' or 'pipenv run pytest'

 if you want to run the test propperly, make sure to follow this list:
 * run 'pipenv run python3 run_testindicies.py' to start the test containers. You see the port number in the output.
 * change the config_class in app/__init__.py to 'TestConfig' instead of 'Config'
 * run pipenv run flask run --debug --port 4000
 * enter the port number manually (you prolly have to do that twice if you start it for the first time)
 * run the tests via 'pytest' or 'pipenv run pytest'
"""
import unittest
from requests import post


class DetermineDatatypesTest(unittest.TestCase):

    # @Test
    def test_textsearch(self):
        print("search for entries that contain the word 'measurement data'")
        response = post(f"http://localhost:4000/api/search", json={
            "search_term": "measurement data"
        })
        if response.status_code != 200:
            self.fail("Invalid response code")
        docIDs = [hit["_source"]["docID"] for hit in response.json()["hits"]["hits"]]
        assert docIDs == [2]

    # @Test
    def test_timerange(self):
        print("search for entries that have been created between January and September of 2023")
        response = post(f"http://localhost:4000/api/search", json={
            "t1": "2023-01-01",
            "t2": "2023-09-09"
        })
        if response.status_code != 200:
            self.fail("Invalid response code")
        docIDs = [hit["_source"]["docID"] for hit in response.json()["hits"]["hits"]]
        assert docIDs == [1, 2]

    # @Test
    def test_keywords(self):
        print("Search for entries form the user 'max")
        response = post(f"http://localhost:4000/api/search", json={
            "field": "author",
            "value": "max"
        })
        if response.status_code != 200:
            self.fail("Invalid response code")
        docIDs = [hit["_source"]["docID"] for hit in response.json()["hits"]["hits"]]
        assert docIDs == [2]
