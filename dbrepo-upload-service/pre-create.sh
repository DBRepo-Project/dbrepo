#!/bin/bash
REQUEST_RAW=$(cat /dev/stdin)
AUTH_SERVICE_ENDPOINT="${AUTH_SERVICE_ENDPOINT:-http://auth-service:8080}"

echo "[DEBUG] [pre-create hook] request started" >&2
if [ "$(echo "$REQUEST_RAW" | jq '.Event.HTTPRequest.Header | has("Authorization")')" == "false" ]; then
  echo "[ERROR] [pre-create hook] Missing header 'Authorization'" >&2
  echo "[DEBUG] [pre-create hook] raw request: ${REQUEST_RAW}" >&2
  cat <<END
  {
    "RejectUpload": true,
    "HTTPResponse": {
      "StatusCode": 400,
      "Header": {
          "Content-Type": "application/json"
      },
      "Body": "{\"code\":\"error.upload.malformed\",\"message\":\"Missing header 'Authorization'\",\"status\":\"BAD_REQUEST\"}"
    }
  }
END
  exit 0
fi

echo "[DEBUG] [pre-create hook] request has 'Authorization' header present" >&2

BEARER="$(echo "$REQUEST_RAW" | jq -r '.Event.HTTPRequest.Header.Authorization[0]')"
echo "[DEBUG] [pre-create hook] attempting to contact ${AUTH_SERVICE_ENDPOINT}" >&2
if [ ! "$(wget -O- --quiet --header "Authorization: ${BEARER}" ${AUTH_SERVICE_ENDPOINT}/realms/dbrepo/protocol/openid-connect/userinfo)" ]; then
  echo "[ERROR] [pre-create hook] Unauthorized" >&2
  cat <<END
  {
    "RejectUpload": true,
    "HTTPResponse": {
      "StatusCode": 401,
      "Header": {
          "Content-Type": "application/json"
      },
      "Body": "{\"code\":\"error.upload.unauthorized\",\"message\":\"Authentication required\",\"status\":\"UNAUTHORIZED\"}"
    }
  }
END
  exit 0
fi

echo "[INFO] [pre-create hook] Authorized" >&2