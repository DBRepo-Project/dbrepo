from py_eureka_client import eureka_client
import py_eureka_client.logger as logger
from time import sleep

logger.set_level("ERROR")
print("Registering at discovery service ...")


def register(first=False):
    eureka_client.init(eureka_server="http://discovery-service:9090/eureka/", app_name="broker-service",
                       instance_ip="broker-service",
                       instance_host="broker-service",
                       instance_port=15672)
    if first:
        print("Service was registered at Eureka server")


if __name__ == "__main__":
    register(first=True)
    while True:
        register()
        sleep(60)
