class MockResponse:
    def __init__(self, status_code, headers=None, json_data=None):
        self.status_code = status_code
        self.headers = headers or {"Content-Type": "application/json"}
        self.json_data = json_data

    def json(self):
        return self.json_data
