"""Interactive credentials and optional ordinary Keycloak self-registration."""
from getpass import getpass
from html.parser import HTMLParser
import json
import os
from pathlib import Path
import secrets
from urllib.parse import urlencode, urljoin, urlsplit, quote

import requests


class Forms(HTMLParser):
    def __init__(self):
        super().__init__()
        self.forms = []
        self.current = None
        self.select = None
        self.option = None
        self.blocked = False

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if any(word in ' '.join(str(v) for v in attrs.values()).lower() for word in ('captcha', 'turnstile')):
            self.blocked = True
        if tag == 'form':
            self.current = {'id': attrs.get('id'), 'action': attrs.get('action', ''), 'fields': {}}
        elif tag == 'input' and self.current is not None and attrs.get('name'):
            if attrs.get('type', '').lower() not in ('submit', 'button', 'checkbox', 'radio'):
                self.current['fields'][attrs['name']] = attrs.get('value', '')
        elif tag == 'select' and self.current is not None:
            self.select = attrs.get('name')
        elif tag == 'option' and self.select:
            value = attrs.get('value', '')
            if self.select not in self.current['fields'] or 'selected' in attrs:
                self.current['fields'][self.select] = value

    def handle_endtag(self, tag):
        if tag == 'select': self.select = None
        if tag == 'form' and self.current is not None:
            self.forms.append(self.current)
            self.current = None


def same_origin(base, target):
    b, t = urlsplit(base), urlsplit(target)
    return (b.scheme, b.hostname, b.port) == (t.scheme, t.hostname, t.port) and not t.username and not t.password


def session_get(session, base, url):
    for _ in range(8):
        if not same_origin(base, url): raise RuntimeError('Authentication redirected to a different origin; use an existing account')
        response = session.get(url, timeout=(5, 30), allow_redirects=False)
        if response.status_code not in (301, 302, 303, 307, 308): return response
        url = urljoin(url, response.headers['Location'])
    raise RuntimeError('Too many authentication redirects')


def private_config(path, config, create=False):
    path = Path(path)
    if not path.parent.exists(): path.parent.mkdir(parents=True, mode=0o700)
    flags = os.O_WRONLY | os.O_CREAT | (os.O_EXCL if create else os.O_TRUNC)
    fd = os.open(path, flags, 0o600)
    with os.fdopen(fd, 'w') as stream:
        os.fchmod(stream.fileno(), 0o600)
        json.dump(config, stream, indent=2)


def verify_account(base, credentials, ca_bundle=True):
    response = requests.get(base + '/api/v1/user/' + quote(credentials['username'], safe=''),
                            auth=(credentials['username'], credentials['password']),
                            timeout=(5, 30), verify=ca_bundle, allow_redirects=False)
    if response.status_code != 200:
        raise RuntimeError(f'Account login was not confirmed (HTTP {response.status_code}); verification or a required action may be pending')


def register_account(base, credentials, email_domain, ca_bundle=True):
    session = requests.Session()
    session.verify = ca_bundle
    url = base + '/realms/dbrepo/protocol/openid-connect/registrations?' + urlencode({
        'client_id': 'dbrepo-client', 'redirect_uri': base + '/auth/keycloak/callback',
        'response_type': 'code', 'response_mode': 'form_post', 'scope': 'openid profile email'})
    response = session_get(session, base, url)
    parser = Forms(); parser.feed(response.text)
    if parser.blocked: raise RuntimeError('Registration requires a CAPTCHA; create an account manually')
    form = next((f for f in parser.forms if f['id'] == 'kc-register-form'), None)
    if response.status_code != 200 or form is None:
        raise RuntimeError('Ordinary Keycloak registration is unavailable; use an existing account')
    action = urljoin(response.url, form['action'])
    if not same_origin(base, action): raise RuntimeError('Registration action has a different origin')
    defaults = {'username': credentials['username'], 'password': credentials['password'],
                'password-confirm': credentials['password'], 'email': credentials['username'] + '@' + email_domain,
                'firstName': 'Client', 'lastName': 'Test', 'theme': 'light', 'language': 'en'}
    fields = {k: defaults.get(k, v) for k, v in form['fields'].items()}
    session.post(action, data=fields, timeout=(5, 30), allow_redirects=False).close()
    # No admin attributes, realm roles, email verification bypass or OIDC secret are used.
    verify_account(base, credentials, ca_bundle)


def grafana_login(base, credentials, ca_bundle=True):
    session = requests.Session(); session.verify = ca_bundle
    response = session_get(session, base, base + '/dashboard/login/generic_oauth')
    parser = Forms(); parser.feed(response.text)
    if parser.blocked: raise RuntimeError('Grafana login requires a CAPTCHA or manual action')
    form = next((f for f in parser.forms if 'username' in f['fields'] and 'password' in f['fields']), None)
    if form is None: raise RuntimeError('Grafana OAuth login form is unavailable')
    action = urljoin(response.url, form['action'])
    if not same_origin(base, action): raise RuntimeError('Grafana authentication action has a different origin')
    response = session.post(action, data={**form['fields'], 'username': credentials['username'],
                            'password': credentials['password'], 'credentialId': ''},
                            timeout=(5, 30), allow_redirects=False)
    if response.status_code in (301, 302, 303):
        response = session_get(session, base, urljoin(response.url, response.headers['Location']))
    else:
        auto = Forms(); auto.feed(response.text)
        callback = next((f for f in auto.forms if 'code' in f['fields']), None)
        if callback:
            action = urljoin(response.url, callback['action'])
            if not same_origin(base, action): raise RuntimeError('Grafana callback has a different origin')
            response = session.post(action, data=callback['fields'], timeout=(5, 30), allow_redirects=False)
            if response.status_code in (301, 302, 303):
                session_get(session, base, urljoin(response.url, response.headers['Location']))
    profile = session.get(base + '/dashboard/api/user', timeout=(5, 30), allow_redirects=False)
    if profile.status_code != 200 or profile.json().get('login') != credentials['username']:
        raise RuntimeError('Grafana login identity was not confirmed')


def configure(args):
    config = json.loads(args.config.read_text()) if args.config else {}
    if args.endpoint and config.get('endpoint') and config['endpoint'].rstrip('/') != args.endpoint.rstrip('/'):
        raise ValueError('The config belongs to a different endpoint; use target-specific credentials')
    if args.endpoint: config['endpoint'] = args.endpoint.rstrip('/')
    if not config.get('endpoint'): raise ValueError('--endpoint or an endpoint in --config is required')
    base = config['endpoint'].rstrip('/')
    parsed = urlsplit(base)
    if parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError('Endpoint must be an HTTP(S) base URL without credentials, query or fragment')
    if args.container_id: config['container_id'] = args.container_id
    if config.get('oidc', {}).get('client_secret_env'):
        config['oidc']['client_secret'] = os.getenv(config['oidc']['client_secret_env'])
    config['profile'] = args.profile or config.get('profile', 'full' if args.config else 'user')
    if args.ca_bundle: config['ca_bundle'] = str(args.ca_bundle.resolve())
    ca = config.get('ca_bundle', True)
    if ca is not True and (not isinstance(ca,str) or not ca.strip()):
        raise ValueError('ca_bundle must be a trusted CA file path; TLS verification cannot be disabled')
    if args.register_users:
        if args.config or args.username or args.peer_username: raise ValueError('--register-users cannot be combined with existing account settings')
        suffix = secrets.token_hex(6)
        for role in ('owner', 'peer'):
            config[role] = {'username': 'client-live-' + role + '-' + suffix, 'password': secrets.token_urlsafe(24)}
        private_config(args.credentials_output, config, create=True)
        print('Generated credentials saved privately:', args.credentials_output.resolve())
        config['setup'] = []
        for role in ('owner', 'peer'):
            register_account(base, config[role], args.email_domain, ca)
            config['setup'].append({'role': role, 'registration': 'confirmed'})
            private_config(args.credentials_output, config)
    else:
        for role, username, env in [('owner', args.username, args.password_env), ('peer', args.peer_username, args.peer_password_env)]:
            if username and env and not os.getenv(env):
                raise ValueError(f'Password environment variable {env} is not set')
            if username:
                config[role] = {'username': username, 'password': os.getenv(env) if env else getpass(f'{role} password: ')}
        if 'owner' not in config:
            username = input('DBRepo username: ').strip()
            config['owner'] = {'username': username, 'password': getpass('DBRepo password: ')}
    owner=config['owner']
    if not (owner.get('password') or owner.get('token') or os.getenv(owner.get('password_env',''))):
        raise ValueError('Owner credentials are missing; set the configured password environment variable')
    if args.prepare_grafana or args.register_users:
        config.setdefault('setup', [])
        for role in ('owner', 'peer'):
            credential = config.get(role, {}).copy()
            credential['password'] = credential.get('password') or os.getenv(credential.get('password_env', ''))
            if not credential.get('username') or not credential.get('password'): continue
            try:
                grafana_login(base, credential, ca)
                config['setup'].append({'role': role, 'grafana_login': 'confirmed'})
            except (RuntimeError, requests.RequestException, ValueError) as error:
                config['setup'].append({'role': role, 'grafana_login': 'unconfirmed', 'error': type(error).__name__})
    if args.register_users: private_config(args.credentials_output, config)
    return config
