"""Short-lived bearer sessions for live tests; credentials and tokens stay in memory."""
import json
import math
import os
import ssl
import threading
import time
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class BearerSession:
    def __init__(self, endpoint, username, password, oidc, ca_bundle=True, timeout=20):
        self.url = endpoint.rstrip('/') + '/realms/dbrepo/protocol/openid-connect/token'
        self.username, self.password, self.oidc = username, password, oidc
        self.timeout = timeout
        self.http = urllib.request.build_opener(NoRedirect(), urllib.request.HTTPSHandler(
            context=ssl.create_default_context(cafile=ca_bundle if isinstance(ca_bundle, str) else None)))
        self.lock = threading.Lock()
        self.access_token = self.refresh_token = None
        self.expires_at = 0
        self.failed = False

    def token(self):
        with self.lock:
            if self.failed:
                raise RuntimeError('Bearer authentication failed; restart after correcting the account or OIDC configuration')
            if self.access_token and time.monotonic() < self.expires_at:
                return self.access_token
            data = {'client_id': self.oidc.get('client_id', 'dbrepo-client')}
            secret = self.oidc.get('client_secret') or os.getenv(self.oidc.get('client_secret_env', ''))
            if secret:
                data['client_secret'] = secret
            if self.refresh_token:
                data.update(grant_type='refresh_token', refresh_token=self.refresh_token)
            else:
                data.update(grant_type='password', username=self.username, password=self.password)
            request = urllib.request.Request(self.url, method='POST',
                data=urllib.parse.urlencode(data).encode(), headers={'Content-Type': 'application/x-www-form-urlencoded'})
            try:
                with self.http.open(request, timeout=self.timeout) as response:
                    raw = response.read(65537)
                    if len(raw) > 65536:
                        raise ValueError('Token response too large')
                    body = json.loads(raw)
                lifetime = float(body['expires_in'])
                token = body['access_token']
                if not isinstance(token, str) or not token or not math.isfinite(lifetime) or lifetime <= 0:
                    raise ValueError('Invalid token response')
            except (OSError, ValueError, KeyError, TypeError, urllib.error.URLError):
                self.failed = True
                # Never retry credentials or print an identity-service response containing tokens.
                raise RuntimeError('Bearer authentication failed; check the account and OIDC configuration') from None
            self.access_token = token
            self.refresh_token = body.get('refresh_token')
            self.expires_at = time.monotonic() + lifetime * 0.9
            return token
