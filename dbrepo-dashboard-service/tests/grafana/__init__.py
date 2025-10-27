import requests
from testcontainers.core.container import DockerContainer
from testcontainers.core.waiting_utils import wait_for_logs, wait_container_is_ready


class GrafanaContainer(DockerContainer):
    MGMT_PORT = 3000

    def __init__(self, image: str = "bitnamilegacy/grafana:11", **kwargs) -> None:
        super().__init__(image=image, **kwargs)
        self.with_exposed_ports(self.MGMT_PORT)

    def get_url(self) -> str:
        return f"http://{self.get_container_host_ip()}:{self.get_exposed_port(self.MGMT_PORT)}"

    @wait_container_is_ready(requests.exceptions.ConnectionError, requests.exceptions.ReadTimeout)
    def _readiness_probe(self) -> None:
        try:
            response = requests.get(f"{self.get_url()}/api/health", timeout=1)
        except requests.exceptions.ConnectionError:
            response = requests.get(f"{self.get_url()}/healthz", timeout=1)
        response.raise_for_status()
        wait_for_logs(self, "HTTP Server Listen")

    def start(self) -> "GrafanaContainer":
        super().start()
        self._readiness_probe()
        return self
