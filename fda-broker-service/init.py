from py_eureka_client import eureka_client
import py_eureka_client.logger as logger
import datetime

logger.set_level("ERROR")


def register():
    eureka_client.init(eureka_server="http://discovery-service:9090/eureka/",
                       app_name="broker-service",
                       instance_ip="broker-service",
                       instance_host="broker-service",
                       instance_port=15672)
    log("Service registered")


def log(message):
    date = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"{date} LOG: {message}")


if __name__ == "__main__":
    log("Registering at discovery service ...")
    register()
