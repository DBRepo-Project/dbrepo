import requests as rq
import os


class Dockerhub:
    """A simple Dockerhub client"""
    baseurl = "https://hub.docker.com"
    username = ""
    registry = os.getenv("CI_REGISTRY_URL", "docker.io")
    workpath = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    headers = {
        "Content-Type": "application/json",
        "Authorization": None
    }

    def __init__(self):
        self.username = os.getenv("CI_REGISTRY_USER", "mweise")
        print("docker username: %s" % self.username)
        response = rq.post(self.baseurl + "/v2/users/login", {
            "username": self.username,
            "password": os.getenv("CI_REGISTRY_PASSWORD", "rMC7ysaYZJbPqi8vSUM5AzTJsuPH4U")
        })
        if response.status_code == 200:
            self.headers["Authorization"] = "Bearer " + response.json()["token"]
        else:
            raise "Failed to authenticate"

    def modify_description(self, component: {}):
        header = self.__read__(self.workpath + "/_header.md", component)
        footer = self.__read__(self.workpath + "/_footer.md", component)
        body = self.__read__(self.workpath + "/body.md", component)
        url = self.baseurl + "/v2/repositories/dbrepo/" + component["dir"] + "/"
        print("dispatch update: %s" % url)
        response = rq.patch(url, headers=self.headers,
                            json={
                                "description": f"Official DBRepo {component['name']} image",
                                "full_description": header + "\n\n" + body + "\n\n" + footer,
                                "registry": self.registry
                            })
        if response.status_code == 200:
            return response.json()
        else:
            print(response)

    def __read__(self, path, component):
        with open(path, "r") as f:
            return ' '.join([line for line in f.readlines()]).replace(
                "DIR", component["dir"]).replace(
                "DOC", component["doc"]).replace(
                "FRIENDLY_NAME", component["name"]
            )
