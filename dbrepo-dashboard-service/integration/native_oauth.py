"""Native OAuth acceptance with synthetic identities, verified local TLS and signed JWTs.
The backend validates tokens; separate Spring tests cover DBRepo authorization.
"""
import os
import signal
import uuid
import base64
import datetime
import ipaddress
import hashlib
import importlib.util
import json
import re
import socket
import subprocess
import threading
import time
from html.parser import HTMLParser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit, urljoin
import requests
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa, padding

ROOT = Path(os.environ['DBREPO_NATIVE_FIXTURE_DIR'])
# Local fixture CA, explicitly trusted by the HTTP client; no TLS verification bypass.
private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
subject = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'Synthetic native OAuth fixture')])
certificate = (x509.CertificateBuilder().subject_name(subject).issuer_name(subject)
    .public_key(private_key.public_key()).serial_number(x509.random_serial_number())
    .not_valid_before(datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=1))
    .not_valid_after(datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=1))
    .add_extension(x509.BasicConstraints(ca=True, path_length=None), critical=True)
    .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address('127.0.0.1')), x509.DNSName('localhost')]), critical=False)
    .sign(private_key, hashes.SHA256()))
(ROOT / 'fixture.pem').write_bytes(certificate.public_bytes(serialization.Encoding.PEM))
(ROOT / 'fixture-key.pem').write_bytes(private_key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
(ROOT / 'fixture-key.pem').chmod(0o600)
WORK = Path(__file__).resolve().parents[2]
PREFIX = 'dbrepo-oauth-test-' + uuid.uuid4().hex[:10]
ADMIN = ('probeadmin', 'synthetic-native-admin-password')
SECRET = 'synthetic-dedicated-oauth-secret-32-characters'
CLIENT = 'dbrepo-grafana'
RESULTS, CALLS, STARTED = [], [], []
DATA_URL = os.getenv('DBREPO_NATIVE_DATA_URL')
SOURCE_UID = 'dbrepojson0' if DATA_URL else 'native-oauth'
def stop_fixture(*_):
    raise SystemExit(1)
signal.signal(signal.SIGTERM, stop_fixture)
KEYS = {}

def record(event, **values):
    item = {'event': event, **values}
    RESULTS.append(item)
    print(json.dumps(item), flush=True)

def session():
    s = requests.Session(); s.trust_env = False; s.verify = str(ROOT / 'fixture.pem')
    return s

def port():
    with socket.socket() as s:
        s.bind(('127.0.0.1', 0)); return s.getsockname()[1]

def b64(value):
    return base64.urlsafe_b64decode(value + '=' * (-len(value) % 4))

def validate(token):
    head, data, signature = token.split('.')
    header, claims = json.loads(b64(head)), json.loads(b64(data))
    assert header['alg'] == 'RS256'
    key = KEYS[header['kid']]
    public = rsa.RSAPublicNumbers(int.from_bytes(b64(key['e']), 'big'), int.from_bytes(b64(key['n']), 'big')).public_key()
    public.verify(b64(signature), (head + '.' + data).encode(), padding.PKCS1v15(), hashes.SHA256())
    assert claims['iss'] == PUBLIC + '/realms/dbrepo'
    assert claims['azp'] == CLIENT and claims['exp'] > time.time()
    return claims

class Backend(BaseHTTPRequestHandler):
    def log_message(self, *args): pass
    def do_GET(self):
        header = self.headers.get('Authorization', '')
        claims = None
        try:
            if header.startswith('Bearer '): claims = validate(header[7:])
        except Exception: pass
        item = {'path': self.path, 'scheme': header.partition(' ')[0] if header else None,
                'sub': claims.get('sub') if claims else None,
                'fingerprint': hashlib.sha256(header.encode()).hexdigest()[:12] if header else None,
                'exp': claims.get('exp') if claims else None, 'claim_names': sorted(claims) if claims else []}
        CALLS.append(item)
        code = 200 if claims or self.path == '/public' else 403
        payload = json.dumps([{'marker': 'verified-private-subject' if claims else 'anonymous-public',
                               'subject': claims.get('sub') if claims else 'anonymous'}] if code == 200 else {'error': 'identity required'}).encode()
        self.send_response(code); self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(payload))); self.end_headers(); self.wfile.write(payload)

class LoginForm(HTMLParser):
    def __init__(self): super().__init__(); self.action = None; self.fields = {}
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'form' and attrs.get('id') == 'kc-form-login': self.action = attrs['action']
        if tag == 'input' and attrs.get('type') == 'hidden' and attrs.get('name'): self.fields[attrs['name']] = attrs.get('value', '')

def run_container(name, image, env=None, extra=(), command=()):
    args = ['docker', 'run', '--add-host', 'host.docker.internal:host-gateway', '-d', '--name', name, '--network', PREFIX]
    for key, value in (env or {}).items(): args += ['-e', key + '=' + str(value)]
    args += list(extra) + [image] + list(command)
    STARTED.append(name)
    subprocess.run(args, check=True, stdout=subprocess.DEVNULL, timeout=120)

def healthy(url, timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            if session().get(url, timeout=2).status_code == 200: return
        except requests.RequestException: pass
        time.sleep(1)
    raise RuntimeError('Fixture health timeout')

def kc(method, path, body=None, params=None):
    # Image/plugin startup can outlast a short-lived admin token.
    token = KADMIN.post(KBASE + '/realms/master/protocol/openid-connect/token', data={
        'client_id': 'admin-cli', 'grant_type': 'password', 'username': ADMIN[0], 'password': ADMIN[1]}, timeout=20)
    token.raise_for_status()
    KADMIN.headers['Authorization'] = 'Bearer ' + token.json()['access_token']
    response = KADMIN.request(method, KBASE + '/admin/realms/' + path, json=body, params=params, timeout=20)
    response.raise_for_status(); return response

def user(name, email=None):
    response = kc('POST', 'dbrepo/users', {'username': name, 'enabled': True, 'emailVerified': True,
                'email': email or name + '@synthetic.invalid', 'firstName': 'Synthetic', 'lastName': 'Probe',
                'credentials': [{'type': 'password', 'value': 'synthetic-user-password', 'temporary': False}]})
    return response.headers['Location'].rsplit('/', 1)[1]

def grafana(method, path, payload=None):
    response = session().request(method, PUBLIC + '/dashboard/api' + path, auth=ADMIN, json=payload, timeout=20)
    response.raise_for_status(); return response.json()

def login(label, name):
    s = session()
    entry = s.get(PUBLIC + '/dashboard/d/native-return?forceLogin=true', timeout=20)
    assert urlsplit(entry.url).path == '/dashboard/login'
    redirect_to = parse_qs(urlsplit(entry.url).query)['redirectTo'][0]
    assert urlsplit(redirect_to).path == '/d/native-return'
    response = s.get(PUBLIC + '/dashboard/login/generic_oauth', timeout=20)
    assert urlsplit(response.url).path.startswith('/realms/dbrepo/'), urlsplit(response.url).path
    auth = parse_qs(urlsplit(response.url).query)
    assert auth.get('code_challenge_method') == ['S256'] and len(auth.get('code_challenge', [''])[0]) == 43
    assert auth['redirect_uri'] == [PUBLIC + '/dashboard/login/generic_oauth']
    record('login-cookies', cookies=[{'name': c.name, 'secure': c.secure, 'domain': c.domain, 'path': c.path} for c in s.cookies])
    parser = LoginForm(); parser.feed(response.text)
    assert parser.action, 'Keycloak login form missing'
    response = s.post(urljoin(response.url, parser.action), data={**parser.fields, 'username': name, 'password': 'synthetic-user-password'}, timeout=20)
    info = s.get(PUBLIC + '/dashboard/api/user', timeout=20)
    identity = info.json() if info.status_code == 200 and 'login' in info.json() else None
    if identity:
        assert urlsplit(response.url).path == '/dashboard/', urlsplit(response.url).path
        # Grafana 11.6.16 app.ts restores the saved destination from browser sessionStorage.
        # This HTTP fixture models that one navigation step after verifying the real login.
        response = s.get(PUBLIC + '/dashboard' + redirect_to, timeout=20)
        assert response.status_code == 200
        assert urlsplit(response.url).path == '/dashboard/d/native-return', urlsplit(response.url).path
        dashboard = s.get(PUBLIC + '/dashboard/api/dashboards/uid/native-return', timeout=20)
        assert dashboard.status_code == 200 and dashboard.json()['dashboard']['uid'] == 'native-return'
    record('login', case=label, status=info.status_code, final_path=urlsplit(response.url).path,
           user={k: identity.get(k) for k in ['id', 'login', 'isGrafanaAdmin']} if identity else None,
           pkce_s256=True, exact_callback=True)
    return s, identity

def query(s, path):
    before = len(CALLS)
    response = s.post(PUBLIC + '/dashboard/api/ds/query', json={
        'queries': [{'refId': 'A', 'datasource': {'uid': SOURCE_UID, 'type': 'yesoreyeram-infinity-datasource'},
                     'type': 'json', 'source': 'url', 'url': BACKEND + path, 'parser': 'backend',
                     'format': 'table', 'url_options': {'method': 'GET'}, 'root_selector': ''}],
        'from': 'now-1h', 'to': 'now'}, timeout=30)
    calls = CALLS[before:]
    record('query', path=path, status=response.status_code, private_marker='verified-private-subject' in response.text,
           calls=calls)
    return response, calls

backend = ThreadingHTTPServer(('0.0.0.0', 0), Backend)
threading.Thread(target=backend.serve_forever, daemon=True).start()
PUBLIC = 'https://127.0.0.1:' + str(os.getenv('DBREPO_NATIVE_GATEWAY_PORT') or port())
KBASE = 'http://127.0.0.1:' + str(os.getenv('DBREPO_NATIVE_KEYCLOAK_PORT') or port())
BACKEND = DATA_URL or ('http://host.docker.internal:' + str(backend.server_port))
try:
    subprocess.run(['docker', 'network', 'create', PREFIX], check=True, stdout=subprocess.DEVNULL, timeout=20)
    run_container(PREFIX + '-kc', 'quay.io/keycloak/keycloak:26.4.4', {
        'KC_BOOTSTRAP_ADMIN_USERNAME': ADMIN[0], 'KC_BOOTSTRAP_ADMIN_PASSWORD': ADMIN[1],
        'KC_HOSTNAME': PUBLIC, 'KC_HTTP_ENABLED': 'true', 'KC_PROXY_HEADERS': 'xforwarded'},
        extra=['--network-alias', 'auth-service', '-p', '127.0.0.1:' + urlsplit(KBASE).port.__str__() + ':8080'], command=['start-dev'])
    healthy(KBASE + '/realms/master/.well-known/openid-configuration')
    KADMIN = session()
    kc('POST', '', {'realm': 'dbrepo', 'enabled': True, 'sslRequired': 'none', 'accessTokenLifespan': 300 if DATA_URL else 12,
                   'ssoSessionIdleTimeout': 300, 'ssoSessionMaxLifespan': 600, 'editUsernameAllowed': True})
    spec = importlib.util.spec_from_file_location('current_provision', WORK / 'dbrepo-dashboard-service/provision.py')
    provision = importlib.util.module_from_spec(spec); spec.loader.exec_module(provision)
    for attempt in range(2): provision.configure_oauth(KBASE, ADMIN[0], ADMIN[1], CLIENT, SECRET, PUBLIC)
    scopes = kc('GET', 'dbrepo/client-scopes').json()
    basic = next(scope for scope in scopes if scope['name'] == 'basic')
    record('native-basic-scope', mappers=[{'name': m.get('name'), 'type': m.get('protocolMapper'), 'config': m.get('config')} for m in basic.get('protocolMappers', [])])
    config = kc('GET', 'dbrepo/clients', params={'clientId': CLIENT}).json()[0]
    record('configured-default-scopes', scopes=config.get('defaultClientScopes'))
    assert config['redirectUris'] == [PUBLIC + '/dashboard/login/generic_oauth']
    assert config['attributes']['pkce.code.challenge.method'] == 'S256'
    assert not config['directAccessGrantsEnabled'] and not config['serviceAccountsEnabled']
    record('dedicated-client-setup', idempotent=True, exact_callback=True, password_and_service_grants_disabled=True)
    for grant in ['password', 'client_credentials']:
        denial = session().post(KBASE + '/realms/dbrepo/protocol/openid-connect/token', data={
            'grant_type': grant, 'client_id': CLIENT, 'client_secret': SECRET,
            'username': ADMIN[0], 'password': ADMIN[1]}, timeout=20)
        assert denial.status_code in (400, 401)
        record('disabled-grant', grant=grant, status=denial.status_code)
    alice_name, bob_name = ('junit2', 'junit4') if DATA_URL else ('native-alice', 'native-bob')
    alice_id, bob_id = user(alice_name), user(bob_name)
    if DATA_URL:
        kc('POST', 'dbrepo/roles', {'name': 'view-database'})
        role = kc('GET', 'dbrepo/roles/view-database').json()
        for identity in (alice_id, bob_id):
            kc('POST', 'dbrepo/users/' + identity + '/role-mappings/realm', [role])
    user('probeadmin'); user('manual-native', 'manual-identity@synthetic.invalid'); user('email-imposter', 'manual-native@synthetic.invalid')
    KEYS.update({key['kid']: key for key in session().get(KBASE + '/realms/dbrepo/protocol/openid-connect/certs', timeout=20).json()['keys']})
    environment = {
        'GF_SECURITY_ADMIN_USER': ADMIN[0], 'GF_SECURITY_ADMIN_PASSWORD': ADMIN[1],
        'GF_SECURITY_ADMIN_EMAIL': 'probeadmin@synthetic.invalid', 'GF_SERVER_ROOT_URL': PUBLIC + '/dashboard/',
        'GF_AUTH_ANONYMOUS_ENABLED': 'true', 'GF_AUTH_ANONYMOUS_ORG_ROLE': 'Viewer',
        'GF_AUTH_GENERIC_OAUTH_ENABLED': 'true', 'GF_AUTH_GENERIC_OAUTH_NAME': 'DBRepo',
        'GF_AUTH_GENERIC_OAUTH_ALLOW_SIGN_UP': 'true', 'GF_AUTH_GENERIC_OAUTH_CLIENT_ID': CLIENT,
        'GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET': SECRET, 'GF_AUTH_GENERIC_OAUTH_SCOPES': 'openid profile email',
        'GF_AUTH_GENERIC_OAUTH_AUTH_URL': PUBLIC + '/realms/dbrepo/protocol/openid-connect/auth',
        'GF_AUTH_GENERIC_OAUTH_TOKEN_URL': 'http://auth-service:8080/realms/dbrepo/protocol/openid-connect/token',
        'GF_AUTH_GENERIC_OAUTH_API_URL': 'http://auth-service:8080/realms/dbrepo/protocol/openid-connect/userinfo',
        'GF_AUTH_GENERIC_OAUTH_LOGIN_ATTRIBUTE_PATH': 'preferred_username', 'GF_AUTH_GENERIC_OAUTH_EMAIL_ATTRIBUTE_PATH': 'email',
        'GF_AUTH_GENERIC_OAUTH_ROLE_ATTRIBUTE_PATH': "'Viewer'", 'GF_AUTH_GENERIC_OAUTH_USE_PKCE': 'true',
        'GF_AUTH_GENERIC_OAUTH_USE_REFRESH_TOKEN': 'true', 'GF_AUTH_GENERIC_OAUTH_ALLOW_ASSIGN_GRAFANA_ADMIN': 'false',
        'GF_AUTH_OAUTH_ALLOW_INSECURE_EMAIL_LOOKUP': 'false', 'GF_USERS_AUTO_ASSIGN_ORG_ROLE': 'Viewer',
        'GF_USERS_AUTO_ASSIGN_ORG_ID': '1', 'GF_RBAC_PERMISSION_CACHE': 'false', 'GF_LOG_LEVEL': 'warn',
        'GF_PLUGINS_PREINSTALL_DISABLED': 'true',
        'GF_INSTALL_PLUGINS': 'yesoreyeram-infinity-datasource 4.0.0,grafana-opensearch-datasource 2.34.4',
    }
    run_container(PREFIX + '-grafana', 'grafana/grafana:11.6.16', environment,
        extra=['--network-alias', 'dashboard-ui'] + (['-v', os.environ['DBREPO_NATIVE_PLUGINS'] + ':/var/lib/grafana/plugins:ro'] if os.getenv('DBREPO_NATIVE_PLUGINS') else []))
    gateway = (WORK / 'dbrepo-gateway-service/dbrepo.conf').read_text()
    location = re.search(r'    location /dashboard/ \{.*?\n    \}', gateway, re.S).group()
    nginx = 'server { listen 8443 ssl; ssl_certificate /fixture/fixture.pem; ssl_certificate_key /fixture/fixture-key.pem;\n' + location + '\n' + '''
    location /realms { proxy_set_header Host $http_host; proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $http_host; proxy_pass http://auth-service:8080; }
    location /resources { proxy_set_header Host $http_host; proxy_pass http://auth-service:8080; }
}\n'''
    (ROOT / 'nginx.conf').write_text(nginx)
    run_container(PREFIX + '-gateway', 'nginx:1.27.3-alpine3.20-slim', extra=[
        '-p', '127.0.0.1:' + str(urlsplit(PUBLIC).port) + ':8443', '-v', str(ROOT) + ':/fixture:ro', '-v', str(ROOT / 'nginx.conf') + ':/etc/nginx/conf.d/default.conf:ro'])
    healthy(PUBLIC + '/dashboard/api/health')
    record('versions', grafana=grafana('GET', '/health')['version'], keycloak='26.4.4', infinity='4.0.0', gateway='nginx 1.27.3')
    grafana('POST', '/dashboards/db', {'dashboard': {'uid': 'native-return', 'title': 'Native return', 'panels': []}})
    if DATA_URL:
        setup = provision.GrafanaSetup(PUBLIC + '/dashboard', *ADMIN)
        setup.session.verify = str(ROOT / 'fixture.pem')
        setup.configure(BACKEND, 'DBRepo Monitoring', dashboards_dir=WORK / 'dbrepo-dashboard-ui/dashboards/System')
        path = os.environ['DBREPO_NATIVE_DATA_PATH']
        control = os.environ['DBREPO_NATIVE_CONTROL_URL']
        control_header = {'X-Fixture-Control': os.environ['DBREPO_NATIVE_CONTROL_TOKEN']}
        def state(value):
            changed = session().get(control, params={'state': value}, headers=control_header, timeout=10)
            changed.raise_for_status()
        state('private')
        alice, alice_user = login('ordinary-member', alice_name)
        assert alice_user and not alice_user['isGrafanaAdmin']
        response, _ = query(alice, path)
        assert response.status_code == 200 and 'Albury' in response.text
        bob, bob_user = login('ordinary-nonmember', bob_name)
        assert bob_user and not bob_user['isGrafanaAdmin']
        response, _ = query(bob, path)
        assert 'Albury' not in response.text and response.status_code != 200
        response, _ = query(session(), path)
        assert 'Albury' not in response.text and response.status_code != 200
        state('public')
        response, _ = query(session(), path)
        assert response.status_code == 200 and 'Albury' in response.text
        state('revoked')
        response, _ = query(alice, path)
        assert 'Albury' not in response.text and response.status_code != 200
        record('summary', dbrepo_endpoints=True, current_permissions=True,
               private_member=True, anonymous_public=True, revoked_member_denied=True,
               authoritative_metadata_gateway_mock=True)
    else:
        grafana('POST', '/datasources', {'uid': 'native-oauth', 'name': 'Native caller', 'type': 'yesoreyeram-infinity-datasource',
            'access': 'proxy', 'url': BACKEND, 'basicAuth': False, 'basicAuthUser': 'retired-synthetic-service',
            'jsonData': {'auth_method': 'oauthPassThru', 'oauthPassThru': True, 'allowedHosts': [BACKEND]},
            'secureJsonData': {'basicAuthPassword': 'retired-synthetic-password'}})
        manual_id = grafana('POST', '/admin/users', {'login': 'manual-native', 'email': 'manual-native@synthetic.invalid',
            'name': 'Synthetic manual', 'password': 'synthetic-manual-password'})['id']
        alice, alice_user = login('alice', 'native-alice')
        assert alice_user and not alice_user['isGrafanaAdmin']
        response, calls = query(alice, '/private')
        assert response.status_code == 200 and calls[-1]['sub'] == alice_id
        first_token = calls[-1]
        bob, bob_user = login('bob', 'native-bob')
        assert bob_user and bob_user['id'] != alice_user['id']
        response, calls = query(bob, '/private')
        assert response.status_code == 200 and calls[-1]['sub'] == bob_id
        anonymous = session()
        response, calls = query(anonymous, '/private')
        assert 'verified-private-subject' not in response.text and calls[-1]['scheme'] is None
        response, calls = query(anonymous, '/public')
        assert response.status_code == 200 and calls[-1]['scheme'] is None
        # Token expires after 12 seconds; check native refresh, then revocation at next expiry.
        delay = max(0, first_token['exp'] - time.time() + 2)
        record('waiting-for-access-token-expiry', seconds=round(delay, 1))
        time.sleep(delay)
        response, calls = query(alice, '/private')
        assert response.status_code == 200 and calls[-1]['sub'] == alice_id and calls[-1]['fingerprint'] != first_token['fingerprint']
        record('native-token-refresh', renewed=True, same_subject=True)
        latest_expiry = calls[-1]['exp']
        kc('POST', 'dbrepo/users/' + alice_id + '/logout')
        time.sleep(max(0, latest_expiry - time.time() + 2))
        response, calls = query(alice, '/private')
        assert 'verified-private-subject' not in response.text and not any(c['sub'] or c['scheme'] == 'Basic' for c in calls)
        record('revoked-refresh-session', private_access_denied=True, status=response.status_code)
        for label, name in [('local-admin-collision', 'probeadmin'), ('local-manual-collision', 'manual-native'), ('email-only-collision', 'email-imposter')]:
            _, collision = login(label, name)
            assert collision is None, 'Collision unexpectedly logged in'
        # Rename the same actual Keycloak subject, then create another subject with the old name.
        kc('PUT', 'dbrepo/users/' + bob_id, {'username': 'native-bob-renamed', 'email': 'bob-renamed@synthetic.invalid'})
        renamed, renamed_user = login('same-subject-rename', 'native-bob-renamed')
        assert renamed_user and renamed_user['id'] == bob_user['id']
        kc('PUT', 'dbrepo/users/' + bob_id, {'username': 'native-bob-displaced', 'email': 'bob-displaced@synthetic.invalid'})
        user('native-bob-renamed', 'bob-renamed@synthetic.invalid')
        _, collision = login('different-subject-existing-oauth-login', 'native-bob-renamed')
        assert collision is None
        record('summary', real_keycloak=True, signed_jwt_verified=True, subpath_callback=True,
            per_caller_bearer=True, refresh=True, revoked_refresh_denied=True, collisions_fail_closed=True,
            same_subject_rename=True, dbrepo_endpoints=False, production_tls=False)
finally:
    (ROOT / 'results.json').write_text(json.dumps(RESULTS, indent=2))
    for args in ([['docker', 'rm', '-f', name] for name in reversed(STARTED)] +
                 [['docker', 'network', 'rm', PREFIX]]):
        try:
            subprocess.run(args, check=False, stdout=subprocess.DEVNULL, timeout=10)
        except subprocess.TimeoutExpired:
            print('Fixture cleanup timed out for owned resource: ' + args[-1], flush=True)
    backend.shutdown()
