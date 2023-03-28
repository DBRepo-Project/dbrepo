import requests as rq
import py_eureka_client.logger as logger
import datetime

logger.set_level("ERROR")


def get_cert() -> str:
    body = rq.get("http://gateway-service:9095/api/auth/realms/dbrepo/protocol/openid-connect/certs").json()
    for key in body["keys"]:
        if key["alg"] != "RS256":
            continue
        cert = "-----BEGIN CERTIFICATE-----\n"
        cert += key["x5c"][0]
        cert += "\n-----END CERTIFICATE-----"
        return cert


def get_pubkey() -> str:
    body = rq.get("http://gateway-service:9095/api/auth/realms/dbrepo").json()
    pubkey = "-----BEGIN RSA PUBLIC KEY-----\n"
    pubkey += body["public_key"]
    pubkey += "\n-----END RSA PUBLIC KEY-----"
    return pubkey


def write_file(path, content):
    with open(path, 'w') as f:
        f.write(content)


def log(message):
    date = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"{date} LOG: {message}")


if __name__ == "__main__":
    log("Retrieving certificate ...")
    pem = get_cert()
    pubkey = get_pubkey()
    write_file("/app/cert.pem", pem)
    log("saved cert to /app/cert.pem")
    write_file("/app/pubkey.pem", pubkey)
    log("saved cert to /app/pubkey.pem")
